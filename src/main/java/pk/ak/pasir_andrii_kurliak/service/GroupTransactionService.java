package pk.ak.pasir_andrii_kurliak.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.ak.pasir_andrii_kurliak.dto.GroupTransactionDTO;
import pk.ak.pasir_andrii_kurliak.model.*;
import pk.ak.pasir_andrii_kurliak.repository.DebtRepository;
import pk.ak.pasir_andrii_kurliak.repository.GroupRepository;
import pk.ak.pasir_andrii_kurliak.repository.MembershipRepository;
import pk.ak.pasir_andrii_kurliak.repository.TransactionRepository;
import pk.ak.pasir_andrii_kurliak.websocket.GroupNotificationHandler;

import java.util.*;

@Service
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final TransactionRepository transactionRepository;
    private final GroupNotificationHandler groupNotificationHandler;

    public GroupTransactionService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            MembershipService membershipService,
            TransactionRepository transactionRepository,
            GroupNotificationHandler groupNotificationHandler) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.membershipService = membershipService;
        this.transactionRepository = transactionRepository;
        this.groupNotificationHandler = groupNotificationHandler;
    }

    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono Grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);
        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma członków, nie można dodać transakcji.");
        }

        double amountPerUser = transactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(transactionDTO.getType());

        // 1. Create personal transaction for the current user to update their balance immediately
        TransactionType personalType = expense ? TransactionType.EXPENSE : TransactionType.INCOME;
        Transaction personalTx = new Transaction(
                transactionDTO.getAmount(),
                personalType,
                "Grupa: " + group.getName(),
                transactionDTO.getTitle(),
                currentUser
        );
        transactionRepository.save(personalTx);

        // 2. Create debts and notify other members
        for (Membership member : selectedMembers) {
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())) {
                Debt debt = new Debt();
                // EXPENSE: other members owe the one who paid (currentUser is creditor)
                // INCOME:  currentUser owes others (currentUser is debtor)
                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());
                debtRepository.save(debt);

                // Send WebSocket notification to the participant
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "GROUP_EXPENSE_ADDED");
                payload.put("groupId", group.getId());
                payload.put("groupName", group.getName());
                payload.put("title", transactionDTO.getTitle());
                payload.put("amount", transactionDTO.getAmount());
                payload.put("userShare", amountPerUser);
                payload.put("createdByEmail", currentUser.getEmail());
                payload.put("message", String.format("%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                        currentUser.getEmail(), transactionDTO.getTitle(), group.getName(), amountPerUser));

                groupNotificationHandler.sendNotification(otherUser.getEmail(), payload);
            }
        }
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {
        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }

        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);
        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();

        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException(
                    "Wszyscy wybrani użytkownicy muszą być członkami grupy.");
        }

        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        if (!currentUserSelected) {
            throw new IllegalStateException(
                    "Aktualny użytkownik musi być uczestnikiem transakcji grupowej.");
        }

        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwóch uczestników.");
        }

        return selectedMembers;
    }
}

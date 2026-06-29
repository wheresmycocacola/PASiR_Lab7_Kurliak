import { GroupDebt } from "../../api/groupsApi";

export const canManageDebt = (
  debt: GroupDebt,
  isGroupOwner: boolean,
  currentUserId: string
): boolean =>
  isGroupOwner ||
  String(debt.debtor.id) === currentUserId ||
  String(debt.creditor.id) === currentUserId;

export const canMarkDebtAsPaid = (debt: GroupDebt, currentUserId: string): boolean =>
  String(debt.debtor.id) === currentUserId && !debt.paidByDebtor;

export const canConfirmDebtPayment = (debt: GroupDebt, currentUserId: string): boolean =>
  String(debt.creditor.id) === currentUserId &&
  debt.paidByDebtor &&
  !debt.confirmedByCreditor;

export const getDebtStatus = (debt: GroupDebt): "paid" | "pending" | "open" => {
  if (debt.confirmedByCreditor) return "paid";
  if (debt.paidByDebtor) return "pending";
  return "open";
};

export const getDebtStatusLabel = (debt: GroupDebt): string => {
  if (debt.confirmedByCreditor) return "Spłata potwierdzona";
  if (debt.paidByDebtor) return "Oczekuje na potwierdzenie";
  return "Nieopłacony";
};

export const getDebtStatusClass = (
  debt: GroupDebt,
  styles: Record<string, string>
): string => {
  const status = getDebtStatus(debt);
  const map = {
    paid: styles.statusPaid,
    pending: styles.statusPending,
    open: styles.statusOpen,
  };
  return map[status];
};

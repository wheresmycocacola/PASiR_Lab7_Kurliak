import { useEffect } from "react";
import { toast } from "react-toastify";
import { useAuth } from "../../context/AuthContext";

interface GroupNotification {
  type: "GROUP_EXPENSE_ADDED";
  groupId: number | string;
  groupName: string;
  title: string;
  amount: number;
  userShare: number;
  createdByEmail: string;
  message: string;
}

const getWebSocketUrl = (token: string) => {
  const protocol = globalThis.location.protocol === "https:" ? "wss" : "ws";
  const baseUrl = import.meta.env.VITE_WS_URL || `${protocol}://${globalThis.location.hostname}:8080/ws/group-notifications`;
  return `${baseUrl}?token=${encodeURIComponent(token)}`;
};

const GroupNotificationsListener = () => {
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (!isAuthenticated) return;

    const token = localStorage.getItem("accessToken");
    const jwtPattern = /^[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+$/;
    if (!token || !jwtPattern.test(token)) return;

    const socket = new WebSocket(getWebSocketUrl(token));

    socket.onmessage = (event) => {
      try {
        const notification = JSON.parse(event.data) as GroupNotification;
        if (notification.type === "GROUP_EXPENSE_ADDED") {
          toast.info(notification.message);
        }
      } catch (error) {
        console.error("Nie udało się obsłużyć komunikatu grupowego:", error);
      }
    };

    socket.onerror = (error) => {
      console.error("Błąd połączenia WebSocket z komunikatami grupowymi:", error);
    };

    return () => {
      socket.close();
    };
  }, [isAuthenticated]);

  return null;
};

export default GroupNotificationsListener;

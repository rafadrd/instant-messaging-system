import { useEffect, useState } from "react";
import { Message } from "../../types";

interface UseMessageSSEProps {
  channelId: number;
  onNewMessage: (message: Message) => void;
}

const useMessageSSE = ({ channelId, onNewMessage }: UseMessageSSEProps) => {
  const [sseError, setSseError] = useState<string | null>(null);

  useEffect(() => {
    if (!channelId) return;

    let eventSource: EventSource | null = null;
    let retryTimeout: NodeJS.Timeout;

    const connect = () => {
      const token = localStorage.getItem("token");
      const url = `/api/channels/${channelId}/listen?access_token=${token}`;

      eventSource = new EventSource(url, {
        withCredentials: true,
      });

      eventSource.onopen = () => setSseError(null);

      eventSource.addEventListener("new-message", (event) => {
        try {
          const message = JSON.parse(event.data);
          onNewMessage(message);
        } catch (error) {
          setSseError("Failed to parse SSE message.");
        }
      });

      eventSource.onmessage = (event) => {
        if (event.data.startsWith(":")) {
          console.log("Received keep-alive:", event.data);
        }
      };

      eventSource.onerror = () => {
        setSseError("Failed to connect to SSE stream.");
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
        retryTimeout = setTimeout(connect, 5000);
      };
    };

    connect();

    return () => {
      if (eventSource) {
        eventSource.close();
      }
      clearTimeout(retryTimeout);
    };
  }, [channelId, onNewMessage]);

  return { sseError };
};

export default useMessageSSE;

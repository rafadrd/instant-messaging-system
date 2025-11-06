import { useQuery } from "@tanstack/react-query";
import { Message } from "../../types";
import { fetchMessages } from "../../api/messages";

const useFetchMessages = (channelId: number | null) => {
  const { data, isLoading, error } = useQuery<Message[], Error>({
    queryKey: ["messages", channelId],
    queryFn: () => fetchMessages(channelId!),
    enabled: !!channelId,
  });

  return {
    messages: data ?? [],
    loading: isLoading,
    error: error?.message || null,
  };
};

export default useFetchMessages;

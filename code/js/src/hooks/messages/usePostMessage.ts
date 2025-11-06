import { useMutation } from "@tanstack/react-query";
import { postNewMessage } from "../../api/messages";

const usePostMessage = (channelId: number | null) => {
  const mutation = useMutation<any, Error, { content: string }>({
    mutationFn: ({ content }) => {
      if (!channelId) {
        return Promise.reject(new Error("Invalid channel ID."));
      }
      const trimmedMessage = content.trim();
      if (!trimmedMessage) {
        return Promise.reject(new Error("Message cannot be empty."));
      }
      return postNewMessage(channelId, trimmedMessage);
    },
  });

  return {
    handlePostMessage: (content: string) => mutation.mutate({ content }),
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};

export default usePostMessage;

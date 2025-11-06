import { useMutation, useQueryClient } from "@tanstack/react-query";
import { joinChannel, joinChannelByToken } from "../../api/channels";

export const useJoinPublicChannel = (channelId: number) => {
  const queryClient = useQueryClient();

  const mutation = useMutation<void, Error, number>({
    mutationFn: (id) => joinChannel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["channels", "user"] });
      queryClient.invalidateQueries({ queryKey: ["channels", "search"] });
    },
  });

  return {
    handleJoin: mutation.mutate,
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};

export const useJoinPrivateChannel = () => {
  const queryClient = useQueryClient();

  const mutation = useMutation<void, Error, string>({
    mutationFn: (token) => joinChannelByToken(token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["channels", "user"] });
    },
  });

  return {
    handleJoin: mutation.mutate,
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};
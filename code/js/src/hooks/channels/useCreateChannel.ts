import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { createChannel } from "../../api/channels";
import { ChannelInput, Channel } from "../../types";

const useCreateChannel = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const mutation = useMutation<Channel, Error, ChannelInput>({
    mutationFn: createChannel,
    onSuccess: (newChannel) => {
      queryClient.invalidateQueries({ queryKey: ["channels", "user"] });
      navigate(`/channels/${newChannel.id}`);
    },
  });

  return {
    createChannel: mutation.mutate,
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};

export default useCreateChannel;

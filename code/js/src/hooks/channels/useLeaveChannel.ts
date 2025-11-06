import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { leaveChannel } from "../../api/channels";

const useLeaveChannel = (channelId: number | null) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const mutation = useMutation<string, Error>({
    mutationFn: () => {
      if (!channelId) throw new Error("Invalid channel ID.");
      return leaveChannel(channelId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["channels", "user"] });
      queryClient.invalidateQueries({ queryKey: ["channel", channelId] });
      navigate("/channels");
    },
  });

  return {
    leaveChannel: mutation.mutate,
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};

export default useLeaveChannel;

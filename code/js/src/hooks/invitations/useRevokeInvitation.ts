import { useMutation, useQueryClient } from "@tanstack/react-query";
import { revokeInvitations } from "../../api/invitations";

const useRevokeInvitation = (channelId: number) => {
  const queryClient = useQueryClient();

  const mutation = useMutation<string, Error, { invitationId: number }>({
    mutationFn: ({ invitationId }) => revokeInvitations(channelId, invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invitations", channelId] });
    },
  });

  return {
    revokeInvitation: (invitationId: number) => mutation.mutate({ invitationId }),
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};

export default useRevokeInvitation;

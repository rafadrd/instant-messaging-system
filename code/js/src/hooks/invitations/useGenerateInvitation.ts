import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Invitation, InvitationInput } from "../../types";
import { createInvitation } from "../../api/invitations";

const useGenerateInvitation = () => {
  const [invitation, setInvitation] = useState<Invitation | null>(null);

  const mutation = useMutation<Invitation, Error, InvitationInput>({
    mutationFn: createInvitation,
    onSuccess: (data) => {
      setInvitation(data);
    },
  });

  return {
    generateInvitation: mutation.mutate,
    invitation,
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};

export default useGenerateInvitation;

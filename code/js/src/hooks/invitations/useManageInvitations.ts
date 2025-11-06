import { useQuery } from "@tanstack/react-query";
import { Invitation } from "../../types";
import { fetchInvitations } from "../../api/invitations";

const useManageInvitations = (channelId: number) => {
  const { data, isLoading, error, refetch } = useQuery<Invitation[], Error>({
    queryKey: ["invitations", channelId],
    queryFn: () => fetchInvitations(channelId),
    enabled: !!channelId,
  });

  return {
    invitations: data ?? [],
    loading: isLoading,
    error: error?.message || null,
    reloadInvitations: refetch,
  };
};

export default useManageInvitations;
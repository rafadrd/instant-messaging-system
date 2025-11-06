import { useQuery } from "@tanstack/react-query";
import { fetchMemberAccessType } from "../../api/channels";
import useAuth from "../auth/useAuth";

const useFetchAccessType = (channelId: number | null) => {
  const { user } = useAuth();
  const userId = user?.id;

  return useQuery<string, Error>({
    queryKey: ["accessType", channelId, userId],
    queryFn: () => fetchMemberAccessType(userId!, channelId!),
    enabled: !!channelId && !!userId,
  });
};

export default useFetchAccessType;

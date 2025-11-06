import { useQuery } from "@tanstack/react-query";
import { fetchUserChannels } from "../../api/users";
import { Channel } from "../../types";
import useAuth from "../auth/useAuth";

const useFetchUserChannels = () => {
  const { user } = useAuth();
  return useQuery<Channel[], Error>({
    queryKey: ["channels", "user"],
    queryFn: fetchUserChannels,
    enabled: !!user,
  });
};

export default useFetchUserChannels;

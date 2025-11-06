import { useQuery } from "@tanstack/react-query";
import { fetchChannelDetails } from "../../api/channels";
import { Channel } from "../../types";

const useFetchChannel = (channelId: number | null) => {
  return useQuery<Channel, Error>({
    queryKey: ["channel", channelId],
    queryFn: () => fetchChannelDetails(channelId!),
    enabled: !!channelId,
  });
};

export default useFetchChannel;

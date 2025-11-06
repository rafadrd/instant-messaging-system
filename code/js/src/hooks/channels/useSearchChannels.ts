import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Channel } from "../../types";
import { searchChannels } from "../../api/channels";

const useSearchChannels = () => {
  const [query, setQuery] = useState<string>("");

  const {
    data: channels = [],
    isLoading: loadingSearch,
    error: errorSearch,
  } = useQuery<Channel[], Error>({
    queryKey: ["channels", "search", query],
    queryFn: () => searchChannels(query),
    // enabled: query.trim().length > 0,
  });

  return {
    query,
    setQuery,
    channels,
    loadingSearch,
    errorSearch: errorSearch?.message || null,
  };
};

export default useSearchChannels;

import * as React from "react";
import { ChangeEvent, useCallback } from "react";
import "../CSS/Channels.css";
import ChannelBox from "../../components/ChannelBox";
import LoadingSpinner from "../../components/LoadingSpinner";
import useSearchChannels from "../../hooks/channels/useSearchChannels";
import useFetchUserChannels from "../../hooks/channels/useFetchUserChannels";
import { Channel } from "../../types";

const SearchChannelsPage = () => {
  const {
    data: userChannels = [],
    isLoading: loadingUserChannels,
    error: errorUserChannels,
  } = useFetchUserChannels();

  const {
    query,
    setQuery,
    channels: searchedChannels,
    loadingSearch,
    errorSearch,
  } = useSearchChannels();

  const handleSearchChange = (e: ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
  };

  const isUserJoined = useCallback(
    (channelId: number) => {
      return userChannels.some((channel: Channel) => channel.id === channelId);
    },
    [userChannels],
  );

  return (
    <div className="channels-container">
      <h2 className="channels-title">Search for Channels</h2>
      <form
        onSubmit={(e) => e.preventDefault()}
        className="channels-form"
        aria-label="Search Channels"
      >
        <input
          type="text"
          placeholder="Enter channel name"
          value={query}
          onChange={handleSearchChange}
          className="channels-input"
          aria-label="Channel Name"
        />
        <button
          type="submit"
          disabled={loadingSearch}
          className="channels-button"
          aria-disabled={loadingSearch}
        >
          {loadingSearch ? "Searching..." : "Search"}
        </button>
      </form>

      {errorUserChannels && (
        <div className="channels-error" role="alert">
          {errorUserChannels.message}
        </div>
      )}

      {errorSearch && (
        <div className="channels-error" role="alert">
          {errorSearch}
        </div>
      )}

      {(loadingUserChannels || loadingSearch) && <LoadingSpinner />}

      {!loadingSearch && searchedChannels.length > 0 && (
        <div className="channels-grid" role="list">
          {searchedChannels.map((channel) => (
            <ChannelBox
              key={channel.id}
              channel={channel}
              isJoined={isUserJoined(channel.id)}
            />
          ))}
        </div>
      )}

      {!loadingSearch &&
        query.trim() !== "" &&
        searchedChannels.length === 0 && (
          <div className="channels-no-results">No channels found.</div>
        )}
    </div>
  );
};

export default SearchChannelsPage;

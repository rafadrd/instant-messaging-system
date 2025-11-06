import * as React from "react";
import ChannelBox from "../../components/ChannelBox";
import LoadingSpinner from "../../components/LoadingSpinner";
import "../CSS/Channels.css";
import useFetchUserChannels from "../../hooks/channels/useFetchUserChannels";
import { Channel } from "../../types";

const JoinedChannelsPage = () => {
  const { data: channels = [], isLoading: loading, error } = useFetchUserChannels();

  if (loading) return <LoadingSpinner />;
  if (error)
    return (
      <div className="channels-error" role="alert">
        {error.message}
      </div>
    );

  return (
    <div className="channels-container">
      <h2 className="channels-title">My Channels</h2>
      {channels.length === 0 ? (
        <div className="channels-no-results">
          You have not joined any channels yet.
        </div>
      ) : (
        <div className="channels-grid">
          {channels.map((channel: Channel) => (
            <ChannelBox key={channel.id} channel={channel} isJoined />
          ))}
        </div>
      )}
    </div>
  );
};

export default JoinedChannelsPage;

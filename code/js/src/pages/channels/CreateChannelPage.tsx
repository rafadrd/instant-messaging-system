import * as React from "react";
import { FormEvent, useState } from "react";
import "../CSS/Channels.css";
import useCreateChannel from "../../hooks/channels/useCreateChannel";

const CreateChannelPage = () => {
  const { createChannel, loading, error } = useCreateChannel();
  const [channelName, setChannelName] = useState("");
  const [isPublic, setIsPublic] = useState(true);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    createChannel({ name: channelName, isPublic });
  };

  return (
    <div className="create-container">
      <h2 className="channels-title">Create a New Channel</h2>
      <form onSubmit={handleSubmit} className="channels-form">
        <div>
          <label htmlFor="channelName" className="channels-label">
            Channel Name
          </label>
          <input
            id="channelName"
            type="text"
            value={channelName}
            onChange={(e) => setChannelName(e.target.value)}
            required
            minLength={3}
            aria-describedby="channelNameHelp"
            className="channels-input"
            disabled={loading}
          />
          <small id="channelNameHelp">
            Enter a unique name for your channel (minimum 3 characters).
          </small>
        </div>

        <div>
          <fieldset disabled={loading}>
            <legend className="channels-label">Privacy Setting</legend>
            <div>
              <label htmlFor="public">
                <input
                  id="public"
                  type="radio"
                  name="privacy"
                  value="public"
                  checked={isPublic}
                  onChange={() => setIsPublic(true)}
                />
                Public
              </label>
              <label htmlFor="private">
                <input
                  id="private"
                  type="radio"
                  name="privacy"
                  value="private"
                  checked={!isPublic}
                  onChange={() => setIsPublic(false)}
                />
                Private
              </label>
            </div>
          </fieldset>
        </div>

        {error && <div className="channels-error">{error}</div>}

        <button
          type="submit"
          disabled={loading}
          className="channels-button"
          aria-busy={loading}
        >
          {loading ? (
            <>
              <span aria-hidden="true"></span>
              Creating...
            </>
          ) : (
            "Create Channel"
          )}
        </button>
      </form>
    </div>
  );
};

export default CreateChannelPage;

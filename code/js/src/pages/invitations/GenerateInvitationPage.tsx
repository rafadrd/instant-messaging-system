import * as React from "react";
import { ChangeEvent, FormEvent, useState } from "react";
import "../CSS/Channels.css";
import useFetchUserChannels from "../../hooks/channels/useFetchUserChannels";
import useGenerateInvitation from "../../hooks/invitations/useGenerateInvitation";
import { useJoinPrivateChannel } from "../../hooks/channels/useJoinChannel";
import useAuth from "../../hooks/auth/useAuth";
import { Channel } from "../../types";

const GenerateInvitationPage = () => {
  const { user } = useAuth();
  const { data: channels = [] } = useFetchUserChannels();
  const privateChannels = channels.filter((channel: Channel) => !channel.isPublic);

  const {
    invitation,
    generateInvitation,
    loading: generateLoading,
    error: generateError,
  } = useGenerateInvitation();

  const {
    handleJoin,
    loading: joinLoading,
    error: joinError,
  } = useJoinPrivateChannel();

  const [formData, setFormData] = useState({
    channelId: "",
    accessType: "READ_ONLY",
    expiresAt: "",
  });

  const [joinToken, setJoinToken] = useState<string>("");

  const handleGenerateChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleGenerateSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!formData.channelId || !formData.expiresAt) return;
    await generateInvitation({
      channelId: parseInt(formData.channelId, 10),
      accessType: formData.accessType as "READ_ONLY" | "READ_WRITE",
      expiresAt: new Date(formData.expiresAt).toISOString(),
    });
  };

  const handleJoinSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!joinToken.trim()) return;
    await handleJoin(joinToken.trim());
    setJoinToken("");
  };

  if (!user) {
    return (
      <div className="error-message">
        You must be logged in to access this page.
      </div>
    );
  }

  return (
    <div className="create-container">
      <h2 className="channels-title">Generate Invitation</h2>

      {/* Section to Generate an Invitation */}
      <form onSubmit={handleGenerateSubmit} className="channels-form">
        <div>
          <label htmlFor="channelId" className="channels-label">
            Select Channel
          </label>
          <select
            id="channelId"
            name="channelId"
            value={formData.channelId}
            onChange={handleGenerateChange}
            required
            className="channels-input"
            aria-label="Select Channel"
            disabled={generateLoading}
          >
            <option value="">-- Select a Channel --</option>
            {privateChannels.map((channel: Channel) => (
              <option key={channel.id} value={channel.id}>
                {channel.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="channels-label">Access Type</label>
          <div className="channels-input">
            <label>
              <input
                type="radio"
                name="accessType"
                value="READ_ONLY"
                checked={formData.accessType === "READ_ONLY"}
                onChange={handleGenerateChange}
                disabled={generateLoading}
              />
              Read-Only
            </label>
            <label>
              <input
                type="radio"
                name="accessType"
                value="READ_WRITE"
                checked={formData.accessType === "READ_WRITE"}
                onChange={handleGenerateChange}
                disabled={generateLoading}
              />
              Read-Write
            </label>
          </div>
        </div>

        <div>
          <label htmlFor="expiresAt" className="channels-label">
            Expiration Time
          </label>
          <input
            type="datetime-local"
            id="expiresAt"
            name="expiresAt"
            value={formData.expiresAt}
            onChange={handleGenerateChange}
            required
            className="channels-input"
            disabled={generateLoading}
          />
        </div>

        {generateError && <div className="channels-error">{generateError}</div>}

        <button
          type="submit"
          disabled={generateLoading}
          className="channels-button"
        >
          {generateLoading ? "Generating..." : "Generate Invitation"}
        </button>
      </form>

      {invitation && (
        <div className="generate-invitation">
          <h3 className="generate-invitation-title">Invitation Generated</h3>
          <p>
            <strong>Token:</strong> {invitation.token}
          </p>
          <p>
            <strong>Channel:</strong> {invitation.channel.name}
          </p>
          <p>
            <strong>Access Type:</strong> {invitation.accessType}
          </p>
          <p>
            <strong>Expires At:</strong>{" "}
            {new Date(invitation.expiresAt).toLocaleString()}
          </p>
          <button
            onClick={() => navigator.clipboard.writeText(invitation.token)}
            className="generate-invitation-button"
            disabled={generateLoading}
          >
            Copy Token
          </button>
        </div>
      )}

      <hr className="divider" />

      {/* Section to Join a Channel */}
      <h2 className="channels-title">Join Channel Using Invitation</h2>
      <form onSubmit={handleJoinSubmit} className="channels-form">
        <div>
          <label htmlFor="joinToken" className="channels-label">
            Invitation Token
          </label>
          <input
            type="text"
            id="joinToken"
            name="joinToken"
            value={joinToken}
            onChange={(e) => setJoinToken(e.target.value)}
            required
            className="channels-input"
            disabled={joinLoading}
          />
        </div>

        {joinError && <div className="channels-error">{joinError}</div>}

        <button
          type="submit"
          disabled={joinLoading}
          className="channels-button"
        >
          {joinLoading ? "Joining..." : "Join Channel"}
        </button>
      </form>
    </div>
  );
};

export default GenerateInvitationPage;

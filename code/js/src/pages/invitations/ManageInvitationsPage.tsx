import * as React from "react";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Invitation } from "../../types";
import "../CSS/ChannelDetailPage.css";
import useManageInvitations from "../../hooks/invitations/useManageInvitations";
import LoadingSpinner from "../../components/LoadingSpinner";
import useRevokeInvitation from "../../hooks/invitations/useRevokeInvitation";
import useAuth from "../../hooks/auth/useAuth";
import Modal from "../../components/Modal";

const ManageInvitationsPage = () => {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const channelId = parseInt(id || "0", 10);
  const { invitations, loading, error, reloadInvitations } =
    useManageInvitations(channelId);
  const {
    revokeInvitation,
    loading: revokeLoading,
    error: revokeError,
  } = useRevokeInvitation(channelId);

  const [showModal, setShowModal] = useState(false);
  const [selectedInvitation, setSelectedInvitation] = useState<Invitation | null>(null);

  const handleBack = () => {
    navigate(-1);
  };

  const openRevokeModal = (invitation: Invitation) => {
    setSelectedInvitation(invitation);
    setShowModal(true);
  };

  const handleConfirmRevoke = async () => {
    if (selectedInvitation) {
      await revokeInvitation(selectedInvitation.id);
      await reloadInvitations();
    }
    setShowModal(false);
    setSelectedInvitation(null);
  };

  if (!user) {
    return (
      <div className="error-message" role="alert">
        You must be logged in to access this page.
      </div>
    );
  }

  if (loading) {
    return <LoadingSpinner />;
  }

  return (
    <div className="chat-container">
      <header className="chat-header">
        <h1>Manage Invitations</h1>
        <button
          className="leave-channel-button"
          onClick={handleBack}
          aria-label="Go Back"
        >
          &larr; Back
        </button>
      </header>

      <div className="chat-messages">
        {error && (
          <div className="error-message" role="alert">
            {error}
          </div>
        )}

        {revokeError && (
          <div className="error-message" role="alert">
            {revokeError}
          </div>
        )}

        {invitations.filter(
          (invitation: Invitation) => invitation.status === "PENDING",
        ).length === 0 ? (
          <div className="no-messages">
            No pending invitations found for this channel.
          </div>
        ) : (
          <div className="invitations-list">
            {invitations
              .filter((invitation) => invitation.status === "PENDING")
              .map((invitation, index) => (
                <React.Fragment key={invitation.id}>
                  <div className="invitation-card pending">
                    <div className="invitation-header">
                      <div className="invitation-token">
                        <p>
                          <strong>Token:</strong> {invitation.token}
                        </p>
                      </div>
                      <div className="invitation-status pending">
                        <strong>Status:</strong> {invitation.status.replace("_", " ")}
                      </div>
                    </div>
                    <div className="invitation-details">
                      <p>
                        <strong>Access Type:</strong>{" "}
                        {invitation.accessType.replace("_", " ")}
                      </p>
                      <p>
                        <strong>Expires At:</strong>{" "}
                        {new Date(invitation.expiresAt).toLocaleString()}
                      </p>
                    </div>
                    <button
                      onClick={() => openRevokeModal(invitation)}
                      className="revoke-button"
                      aria-label={`Revoke invitation ${invitation.token}`}
                      disabled={revokeLoading}
                    >
                      {revokeLoading ? "Revoking..." : "Revoke"}
                    </button>
                  </div>
                  {index < invitations.length - 1 && (
                    <hr className="invitation-divider"/>
                  )}
                </React.Fragment>
              ))}
          </div>
        )}
      </div>
      {showModal && selectedInvitation && (
        <Modal title="Confirm Revoke" onClose={() => setShowModal(false)}>
          <p>Are you sure you want to revoke this invitation?</p>
          <p><strong>Token:</strong> {selectedInvitation.token}</p>
          <div className="modal-buttons">
            <button
              className="modal-confirm-button"
              onClick={handleConfirmRevoke}
              disabled={revokeLoading}
            >
              {revokeLoading ? "Revoking..." : "Confirm"}
            </button>
            <button
              className="modal-cancel-button"
              onClick={() => setShowModal(false)}
            >
              Cancel
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default ManageInvitationsPage;

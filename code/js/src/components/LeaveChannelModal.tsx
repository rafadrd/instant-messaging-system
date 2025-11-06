import * as React from "react";
import Modal from "./Modal";
import { Channel } from "../types";

interface LeaveChannelModalProps {
  channel: Channel;
  show: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isLeaving: boolean;
  leaveError: string | null;
}

const LeaveChannelModal = ({
  channel,
  show,
  onClose,
  onConfirm,
  isLeaving,
  leaveError,
}: LeaveChannelModalProps) => {
  if (!show) {
    return null;
  }

  return (
    <Modal title="Confirm Leave" onClose={onClose}>
      <p>Are you sure you want to leave the channel "{channel.name}"?</p>
      {leaveError && <p className="modal-error">{leaveError}</p>}
      <div className="modal-buttons">
        <button
          className="modal-confirm-button"
          onClick={onConfirm}
          disabled={isLeaving}
          aria-label="Confirm Leave Channel"
        >
          {isLeaving ? "Leaving..." : "Confirm"}
        </button>
        <button
          className="modal-cancel-button"
          onClick={onClose}
          aria-label="Cancel Leave Channel"
        >
          Cancel
        </button>
      </div>
    </Modal>
  );
};

export default LeaveChannelModal;
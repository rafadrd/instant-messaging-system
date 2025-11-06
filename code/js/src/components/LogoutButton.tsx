import * as React from "react";
import { ReactNode, useState } from "react";
import useLogout from "../hooks/auth/useLogout";
import Modal from "./Modal";

interface LogoutButtonProps {
  children: ReactNode;
  className?: string;
}

const LogoutButton = ({ children, className }: LogoutButtonProps) => {
  const { handleLogout, loading, error } = useLogout();
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const onConfirmLogout = () => {
    setShowConfirmModal(false);
    handleLogout();
  };

  return (
    <div>
      <button
        onClick={() => setShowConfirmModal(true)}
        disabled={loading}
        className={className}
      >
        {loading ? "Logging out..." : children}
      </button>
      {error && <div style={{ color: "red", marginTop: "8px" }}>{error}</div>}

      {showConfirmModal && (
        <Modal title="Confirm Logout" onClose={() => setShowConfirmModal(false)}>
          <p>Are you sure you want to logout?</p>
          <div className="modal-buttons">
            <button
              className="modal-confirm-button"
              onClick={onConfirmLogout}
              disabled={loading}
            >
              Confirm
            </button>
            <button
              className="modal-cancel-button"
              onClick={() => setShowConfirmModal(false)}
            >
              Cancel
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default LogoutButton;

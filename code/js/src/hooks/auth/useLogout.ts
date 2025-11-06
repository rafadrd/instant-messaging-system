import { useState } from "react";
import { useNavigate } from "react-router-dom";
import useAuth from "./useAuth";
import { logoutUser } from "../../api/auth";

const useLogout = () => {
  const { setUser } = useAuth();
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleLogout = async () => {
    setLoading(true);
    try {
      await logoutUser();
      setUser(undefined);
      navigate("/login");
    } catch (err: any) {
      setError(err.message || "Logout failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return { handleLogout, loading, error };
};

export default useLogout;

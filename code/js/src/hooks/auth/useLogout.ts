import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { logoutUser } from "../../api/auth";
import useAuth from "./useAuth";

const useLogout = () => {
  const { setUser } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const mutation = useMutation<void, Error>({
    mutationFn: logoutUser,
    onSuccess: () => {
      setUser(undefined);
      queryClient.clear();
      navigate("/login");
    },
  });

  return {
    handleLogout: mutation.mutate,
    loading: mutation.isPending,
    error: mutation.error?.message || null,
  };
};

export default useLogout;

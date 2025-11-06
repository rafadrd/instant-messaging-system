import * as React from "react";
import { Navigate } from "react-router-dom";
import useAuth from "../hooks/auth/useAuth";
import LoadingSpinner from "./LoadingSpinner";

const RootRedirect = () => {
  const { user, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (user) {
    return <Navigate to="/channels" replace />;
  } else {
    return <Navigate to="/login" replace />;
  }
};

export default RootRedirect;

import * as React from "react";
import { registerReducer } from "../../reducers/formReducers";
import "../CSS/AuthenticationPages.css";
import useAuth from "../../hooks/auth/useAuth";
import { registerUser } from "../../api/auth";
import { RegisterInput } from "../../types";
import AuthForm from "../../components/AuthForm";
import { Navigate } from "react-router-dom";
import LoadingSpinner from "../../components/LoadingSpinner";

const RegisterPage = () => {
  const { user, loading, setUser } = useAuth();

  const handleRegister = async (inputs: RegisterInput) => {
    const registeredUser = await registerUser(inputs);
    setUser(registeredUser);
  };

  if (loading) {
    return <LoadingSpinner />;
  }

  if (user) {
    return <Navigate to="/channels" replace />;
  }

  return (
    <AuthForm<RegisterInput>
      initialInputs={{ username: "", password: "", invitationToken: "" }}
      reducer={registerReducer}
      onSubmit={handleRegister}
      title="Register"
      submitButtonText="Register"
      submittingButtonText="Registering..."
    />
  );
};

export default RegisterPage;

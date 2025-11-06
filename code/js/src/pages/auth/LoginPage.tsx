import * as React from "react";
import { loginReducer } from "../../reducers/formReducers";
import "../CSS/AuthenticationPages.css";
import useAuth from "../../hooks/auth/useAuth";
import { loginUser } from "../../api/auth";
import AuthForm from "../../components/AuthForm";
import { LoginInput } from "../../types";
import { Navigate } from "react-router-dom";
import LoadingSpinner from "../../components/LoadingSpinner";

const LoginPage = () => {
  const { user, loading, setUser } = useAuth();

  const handleLogin = async (inputs: LoginInput) => {
    const user = await loginUser(inputs);
    setUser(user);
  };

  if (loading) {
    return <LoadingSpinner />;
  }

  if (user) {
    return <Navigate to="/channels" replace />;
  }

  return (
    <AuthForm<LoginInput>
      initialInputs={{ username: "", password: "" }}
      reducer={loginReducer}
      onSubmit={handleLogin}
      title="Login"
      submitButtonText="Login"
      submittingButtonText="Logging in..."
    />
  );
};

export default LoginPage;

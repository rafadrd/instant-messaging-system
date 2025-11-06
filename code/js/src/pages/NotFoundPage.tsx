import * as React from "react";
import { useNavigate, Link } from "react-router-dom";
import "./CSS/NotFoundPage.css";

const NotFoundPage = () => {
  const navigate = useNavigate();

  const handleGoHome = () => navigate("/channels");

  return (
    <div className="not-found-container">
      <h1 className="not-found-title">404 - Page Not Found</h1>
      <p className="not-found-message">
        Oops! The page you're looking for doesn't exist.
      </p>
      <button className="not-found-button" onClick={handleGoHome}>
        Go to Home
      </button>
    </div>
  );
};

export default NotFoundPage;

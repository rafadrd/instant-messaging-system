import * as React from "react";
import { FallbackProps } from "react-error-boundary";
import { Link } from "react-router-dom";

const ErrorBoundaryFallback = ({ error, resetErrorBoundary }: FallbackProps) => {
  return (
    <div role="alert" style={{ padding: "20px", textAlign: "center" }}>
      <h2>Something went wrong.</h2>
      <p>We're sorry, but the application encountered an unexpected error.</p>
      <pre style={{ color: "red", margin: "20px 0" }}>{error.message}</pre>
      <button onClick={resetErrorBoundary} style={{ marginRight: "10px" }}>
        Try Again
      </button>
      <Link to="/channels">
        <button>Go to Home</button>
      </Link>
    </div>
  );
};

export default ErrorBoundaryFallback;

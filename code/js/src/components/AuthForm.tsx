import * as React from "react";
import { FormEvent, ChangeEvent, useReducer } from "react";
import { useLocation, Navigate } from "react-router-dom";
import { FormAction, FormState } from "../reducers/formReducers";

type AuthFormProps<Input> = {
  initialInputs: Input;
  reducer: React.Reducer<FormState<Input>, FormAction>;
  onSubmit: (inputs: Input) => Promise<void>;
  title: string;
  submitButtonText: string;
  submittingButtonText: string;
};

const AuthForm = <Input extends {}>({
  initialInputs,
  reducer,
  onSubmit,
  title,
  submitButtonText,
  submittingButtonText,
}: AuthFormProps<Input>) => {
  const location = useLocation();
  const [state, dispatch] = useReducer(reducer, {
    tag: "editing",
    inputs: initialInputs,
    error: undefined,
  } as FormState<Input>);

  if (state.tag === "redirect") {
    const redirectPath = (location.state as any)?.from?.pathname || "/";
    return <Navigate to={redirectPath} replace={true} />;
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (state.tag !== "editing") return;
    dispatch({ type: "submit" });

    try {
      await onSubmit(state.inputs);
      dispatch({ type: "success" });
    } catch (err: any) {
      dispatch({
        type: "error",
        message: err.message || `Operation failed.`,
      });
    }
  };

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    dispatch({ type: "edit", inputName: name, inputValue: value });
  };

  return (
    <div className="authentication-container">
      <div className="authentication-box">
        <h2 className="authentication-title">{title}</h2>
        <form onSubmit={handleSubmit} className="authentication-form">
          <fieldset
            disabled={state.tag !== "editing"}
            className="authentication-fieldset"
          >
            {Object.keys(initialInputs).map((key) => (
              <div className="authentication-input-group" key={key}>
                <label htmlFor={key} className="authentication-label">
                  {key
                    .replace(/([A-Z])/g, " $1")
                    .replace(/^./, (str) => str.toUpperCase())}
                </label>
                <input
                  id={key}
                  type={
                    key.toLowerCase().includes("password") ? "password" : "text"
                  }
                  name={key}
                  value={(state.inputs as any)[key]}
                  onChange={handleChange}
                  required
                  className="authentication-input"
                />
              </div>
            ))}
            <div className="authentication-input-group">
              <button
                type="submit"
                disabled={state.tag !== "editing"}
                className={`authentication-button ${
                  state.tag === "editing" ? "active" : "disabled"
                }`}
              >
                {state.tag === "submitting"
                  ? submittingButtonText
                  : submitButtonText}
              </button>
            </div>
          </fieldset>
          {state.tag === "editing" && state.error && (
            <div className="authentication-error" role="alert">
              {state.error}
            </div>
          )}
        </form>
      </div>
    </div>
  );
};

export default AuthForm;

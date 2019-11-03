package se.sb.epsos.web.auth;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import se.sb.epsos.web.BasePage;
import se.sb.epsos.web.service.NcpServiceException;

public class LoginPage extends BasePage {

    public LoginPage() {
        add(new LoginForm("loginForm"));
    }

    /**
     * Sign in form.
     */
    public final class LoginForm extends StatelessForm<Form<?>> {
        private static final long serialVersionUID = 1L;

        /**
         * remember username
         */
        private boolean rememberMe = true;

        /**
         * Constructor.
         *
         * @param id id of the form component
         */
        public LoginForm(final String id) {
            // sets a compound model on this form, every component without an
            // explicit model will use this model too
            super(id, new CompoundPropertyModel(new ValueMap()));

            // only remember username, not passwords
            add(new TextField<String>("username").setRequired(true).setPersistent(rememberMe).setOutputMarkupId(false));
            add(new PasswordTextField("password").setRequired(true).setOutputMarkupId(false));
            add(new CheckBox("rememberMe", new PropertyModel<Boolean>(this, "rememberMe")));
            add(new AjaxButton("submitButton") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    handleOnSubmit(form.getModelObject());
                }

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(getFeedback());
                }

            });
        }

        private void handleOnSubmit(Object object) {
            if (!rememberMe) {
                getPage().removePersistedFormData(LoginForm.class, true);
            }

            ValueMap values = (ValueMap) object;
            String username = values.getString("username");
            String password = values.getString("password");

            AuthenticatedWebSession session = AuthenticatedWebSession.get();
            if (session.signIn(username, password)) {
                try {
                    getServiceFacade().bindToSession(getSession().getId());
                    getServiceFacade().initServices(LoginPage.this.getSession().getUserDetails());
                } catch (NcpServiceException e) {
                    LOGGER.error("Login failed: ", e);
                    error(getLocalizer().getString("error.login.init", this));
                }
                if (!continueToOriginalDestination()) {
                    setResponsePage(getApplication().getHomePage());
                }
            } else {
                LOGGER.error("Login failed due to bad credentials");
                error(getApplication().getResourceSettings().getLocalizer().getString("error.login", this));
            }
        }

        /**
         * @see org.apache.wicket.Component#getMarkupId()
         */
        @Override
        public String getMarkupId() {
            return getId();
        }

        /**
         * @return true if formdata should be made persistent (cookie) for later logins.
         */
        public boolean getRememberMe() {
            return rememberMe;
        }

        /**
         * Remember form values for later logins?.
         *
         * @param rememberMe true if formdata should be remembered
         */
        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
            ((FormComponent<?>) get("username")).setPersistent(rememberMe);
        }
    }
}

package org.springframework.samples.petclinic.domain;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.samples.petclinic.owner.Owner;

public class OwnerValidation {

	private int counter = 0;

	private UserValidationService usrValSvc;

	private PasswordUtils pwdUtils;

	private Tracer otelTracer;

	private RoleService roleSvc;

	private TwoFactorAuthenticationService twoFASvc;

	public OwnerValidation(Tracer otelTracer) {
		this.pwdUtils = new PasswordUtils();
		this.roleSvc = new RoleService();
		this.otelTracer = otelTracer;
		this.usrValSvc = new UserValidationService();
		this.twoFASvc = new TwoFactorAuthenticationService();
	}

	@WithSpan
	public void ValidateOwnerWithExternalService(Owner owner) {

		this.AuthServiceValidateUser(owner);
	}

	@WithSpan
	// This function and classes were generated by ChatGPT to demonstrate some mock
	// business logic
	public boolean ValidateUserAccess(String usr, String pswd, String sysCode) {

		boolean vldUsr = usrValSvc.vldtUsr(usr);
		if (!vldUsr) {
			return false;
		}

		boolean vldPswd = pwdUtils.vldtPswd(usr, pswd);
		if (!vldPswd) {
			return false;
		}

		boolean vldUsrRole = roleSvc.vldtUsrRole(usr, sysCode);
		if (!vldUsrRole) {
			return false;
		}

		boolean is2FASuccess = twoFASvc.init2FA(usr);
		if (!is2FASuccess) {
			return false;
		}

		boolean is2FATokenValid = false;
		int retry = 0;
		while (retry < 3 && !is2FATokenValid) {
			String token = twoFASvc.getTokenInput();
			is2FATokenValid = twoFASvc.vldtToken(usr, token);
			retry++;
		}

		if (!is2FATokenValid) {
			return false;
		}

		return true;
	}

	@WithSpan
	private synchronized void AuthServiceValidateUser(Owner owner) {
		try {
			Thread.sleep(2000 + (this.counter * 100));
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@WithSpan
	public boolean checkOwnerValidity(Owner owner) {

		return ValidateOwnerUser(owner);
	}

	private boolean ValidateOwnerUser(Owner owner) {

		Span span = otelTracer.spanBuilder("db_access_01").startSpan();

		try {
			for (int i = 0; i < 100; i++) {
				ValidateOwner();
			}
		}
		finally {
			span.end();
		}
		return true;

	}

	private void ValidateOwner() {
		// simulate SpanKind of DB query
		// see
		// https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/database.md
		Span span = otelTracer.spanBuilder("query_users_by_id")
			.setSpanKind(SpanKind.CLIENT)
			.setAttribute("db.system", "other_sql")
			.setAttribute("db.statement", "select * from users where id = :id")
			.startSpan();

		try {
			// delay(1);
		}
		finally {
			span.end();
		}
	}

	public void PerformValidationFlow(Owner owner) {
//		if (owner.getPet("Jerry").isNew()) {
//			ValidateOwner();
//		}
	}

}

using Microsoft.AspNetCore.Authentication;
using System.Security.Claims;
using System.Text.Encodings.Web;
using Microsoft.Extensions.Options;

namespace FleetService.Middleware
{
    public class JwtAuthenticationHandler : AuthenticationHandler<AuthenticationSchemeOptions>
    {
        public JwtAuthenticationHandler(
            IOptionsMonitor<AuthenticationSchemeOptions> options,
            ILoggerFactory logger,
            UrlEncoder encoder)
            : base(options, logger, encoder)
        {
        }

        protected override Task<AuthenticateResult> HandleAuthenticateAsync()
        {
            // El JwtMiddleware ya validó el token y agregó el ClaimsPrincipal al HttpContext
            if (Context.User?.Identity?.IsAuthenticated == true)
            {
                var ticket = new AuthenticationTicket(Context.User, "JwtBearer");
                return Task.FromResult(AuthenticateResult.Success(ticket));
            }

            return Task.FromResult(AuthenticateResult.NoResult());
        }
    }
}
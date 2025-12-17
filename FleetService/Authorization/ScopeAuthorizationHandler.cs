using Microsoft.AspNetCore.Authorization;

namespace FleetService.Authorization
{
    /// <summary>
    /// Handler que verifica si el usuario tiene los scopes (permisos) requeridos
    /// </summary>
    public class ScopeAuthorizationHandler : AuthorizationHandler<ScopeRequirement>
    {
        protected override Task HandleRequirementAsync(
            AuthorizationHandlerContext context,
            ScopeRequirement requirement)
        {
            // Obtener el claim "scope" del token
            var scopeClaim = context.User?.FindFirst("scope")?.Value;

            if (string.IsNullOrEmpty(scopeClaim))
            {
                return Task.CompletedTask;
            }

            // Los scopes vienen separados por espacios en el token
            var userScopes = scopeClaim.Split(' ', StringSplitOptions.RemoveEmptyEntries);

            // Verificar si el usuario tiene al menos uno de los scopes requeridos
            var hasRequiredScope = requirement.Scopes.Any(requiredScope =>
                userScopes.Contains(requiredScope, StringComparer.OrdinalIgnoreCase));

            if (hasRequiredScope)
            {
                context.Succeed(requirement);
            }

            return Task.CompletedTask;
        }
    }
}

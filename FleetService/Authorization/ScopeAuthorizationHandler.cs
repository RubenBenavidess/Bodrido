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
        
        var logger = context.GetType().Assembly.GetName().Name;
        System.Diagnostics.Debug.WriteLine($">>> [ScopeAuthorizationHandler] Buscando scopes. User: {context.User?.Identity?.Name}, Requeridos: {string.Join(", ", requirement.Scopes)}");
        System.Diagnostics.Debug.WriteLine($">>> [ScopeAuthorizationHandler] Scope claim encontrado: {scopeClaim ?? "NULL o VACÍO"}");
        
        // Mostrar todos los claims disponibles para DEBUG
        if (context.User?.Claims != null)
        {
            var allClaims = string.Join(", ", context.User.Claims.Select(c => $"{c.Type}={c.Value}"));
            System.Diagnostics.Debug.WriteLine($">>> [ScopeAuthorizationHandler] Todos los claims: {allClaims}");
        }

        if (string.IsNullOrEmpty(scopeClaim))
        {
            System.Diagnostics.Debug.WriteLine(">>> [ScopeAuthorizationHandler] ❌ NO HAY SCOPE - Acceso DENEGADO");
            return Task.CompletedTask;
        }

        // Los scopes vienen separados por espacios en el token
        var userScopes = scopeClaim.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        System.Diagnostics.Debug.WriteLine($">>> [ScopeAuthorizationHandler] Scopes del usuario: {string.Join(", ", userScopes)}");

        // Verificar si el usuario tiene al menos uno de los scopes requeridos
        var hasRequiredScope = requirement.Scopes.Any(requiredScope =>
            userScopes.Contains(requiredScope, StringComparer.OrdinalIgnoreCase));

        if (hasRequiredScope)
        {
            System.Diagnostics.Debug.WriteLine($">>> [ScopeAuthorizationHandler] ✓ SCOPE VÁLIDO - Acceso PERMITIDO");
            context.Succeed(requirement);
        }
        else
        {
            System.Diagnostics.Debug.WriteLine($">>> [ScopeAuthorizationHandler] ❌ SCOPE INVÁLIDO - Acceso DENEGADO. Requiere: {string.Join(" o ", requirement.Scopes)}, Tiene: {string.Join(" ", userScopes)}");
        }

        return Task.CompletedTask;
    }
    }
}

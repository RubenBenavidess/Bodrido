namespace FleetService.Authorization
{
    /// <summary>
    /// Requisito de autorización basado en scopes (permisos)
    /// </summary>
    public class ScopeRequirement : Microsoft.AspNetCore.Authorization.IAuthorizationRequirement
    {
        public string[] Scopes { get; }

        public ScopeRequirement(params string[] scopes)
        {
            Scopes = scopes ?? throw new ArgumentNullException(nameof(scopes));
        }
    }
}

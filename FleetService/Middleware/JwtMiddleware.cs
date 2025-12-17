using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using Microsoft.IdentityModel.Tokens;

namespace FleetService.Middleware
{
    public class JwtMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly ILogger<JwtMiddleware> _logger;

        public JwtMiddleware(RequestDelegate next, ILogger<JwtMiddleware> logger)
        {
            _next = next;
            _logger = logger;
        }

        public async Task InvokeAsync(HttpContext context, IConfiguration configuration)
        {
            var token = context.Request.Headers["Authorization"].FirstOrDefault()?.Split(" ").Last();

            if (token != null)
            {
                AttachUserToContext(context, token, configuration);
            }

            await _next(context);
        }

        private void AttachUserToContext(HttpContext context, string token, IConfiguration configuration)
        {
            try
            {
                var publicKeyPath = configuration["Jwt:PublicKeyPath"];
                if (string.IsNullOrEmpty(publicKeyPath))
                {
                    _logger.LogWarning("Jwt:PublicKeyPath no está configurado");
                    return;
                }

                var publicKeyPem = File.ReadAllText(publicKeyPath);
                var ecdsa = ECDsa.Create();
                ecdsa.ImportFromPem(publicKeyPem);

                var tokenHandler = new JwtSecurityTokenHandler();
                var validationParameters = new TokenValidationParameters
                {
                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new ECDsaSecurityKey(ecdsa),
                    ValidateIssuer = false,
                    ValidateAudience = false,
                    ValidateLifetime = true,
                    ClockSkew = TimeSpan.Zero
                };

                var principal = tokenHandler.ValidateToken(token, validationParameters, out SecurityToken validatedToken);
                
                context.User = principal;
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Error al validar el token JWT");
            }
        }
    }
}

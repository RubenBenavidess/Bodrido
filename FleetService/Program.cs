using FleetService.Authorization;
using FleetService.Data;
using FleetService.Middleware;
using FleetService.Services;
using FleetService.Services.Implementations;
using Microsoft.AspNetCore.Authorization;
using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter());
    });

var dataSourceBuilder = new Npgsql.NpgsqlDataSourceBuilder(builder.Configuration.GetConnectionString("DefaultConnection"));
dataSourceBuilder.EnableDynamicJson();
var dataSource = dataSourceBuilder.Build();

builder.Services.AddDbContext<FleetContext>(options =>
    options.UseNpgsql(dataSource));

builder.Services.AddScoped<IDriverService, DriverService>();
builder.Services.AddScoped<IVehicleService, VehicleService>();

// Configurar autenticación y autorización
builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = "JwtBearer";
    options.DefaultChallengeScheme = "JwtBearer";
})
.AddScheme<Microsoft.AspNetCore.Authentication.AuthenticationSchemeOptions, JwtAuthenticationHandler>("JwtBearer", options => { });

builder.Services.AddAuthorization(options =>
{
    // Políticas basadas en scopes de fleet
    options.AddPolicy("FleetView", policy =>
        policy.Requirements.Add(new ScopeRequirement("fleet:view")));

    options.AddPolicy("FleetCreate", policy =>
        policy.Requirements.Add(new ScopeRequirement("fleet:create")));

    options.AddPolicy("FleetUpdate", policy =>
        policy.Requirements.Add(new ScopeRequirement("fleet:update")));

    options.AddPolicy("FleetManage", policy =>
        policy.Requirements.Add(new ScopeRequirement("fleet:create", "fleet:update")));
});

// Registrar el handler de autorización
builder.Services.AddSingleton<IAuthorizationHandler, ScopeAuthorizationHandler>();

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    // 1. Definir el esquema de seguridad (Según snippet SwaggerGen-BearerAuthentication)
    options.AddSecurityDefinition("bearer", new OpenApiSecurityScheme
    {
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        Description = "JWT Authorization header using the Bearer scheme."
    });

    // 2. Agregar el requerimiento de seguridad globalmente
    // Nota: La versión 10 usa esta sintaxis con lambda para referenciar el documento
    options.AddSecurityRequirement(document => new OpenApiSecurityRequirement
    {
        [new OpenApiSecuritySchemeReference("bearer", document)] = []
    });
});
var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "Fleet Service API v1");
        c.RoutePrefix = string.Empty;

        c.ConfigObject.AdditionalItems.Add("syntaxHighlight", new Dictionary<string, object>
        {
            ["theme"] = "monokai"
        });
        c.DisplayRequestDuration();
        c.InjectStylesheet("https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/5.11.0/swagger-ui.css");
    });
}

using (var scope = app.Services.CreateScope())
{
    var context = scope.ServiceProvider.GetRequiredService<FleetContext>();
    try
    {
        context.Database.Migrate();
    }
    catch (Exception ex)
    {
        var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();
        logger.LogError(ex, "Error al aplicar migraciones autom�ticamente");
    }
}

app.UseHttpsRedirection();

// Agregar middleware JWT personalizado
app.UseMiddleware<JwtMiddleware>();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();

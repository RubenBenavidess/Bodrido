# Dockerfile para FleetService usando .NET 10
FROM mcr.microsoft.com/dotnet/sdk:10.0-preview AS build
WORKDIR /src
COPY ["FleetService.csproj", "./"]
RUN dotnet restore "FleetService.csproj"
COPY . .
RUN dotnet publish "FleetService.csproj" -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0-preview
WORKDIR /app
COPY --from=build /app/publish .
EXPOSE 8080
ENTRYPOINT ["dotnet", "FleetService.dll"]
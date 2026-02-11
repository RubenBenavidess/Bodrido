using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace FleetService.Migrations
{
    /// <inheritdoc />
    public partial class AddDriverSagaFields : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "created_at",
                table: "Drivers",
                type: "timestamp with time zone",
                nullable: false,
                defaultValueSql: "NOW()");

            migrationBuilder.AddColumn<DateTime>(
                name: "updated_at",
                table: "Drivers",
                type: "timestamp with time zone",
                nullable: false,
                defaultValueSql: "NOW()");

            migrationBuilder.AddColumn<bool>(
                name: "is_validation_completed",
                table: "Drivers",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<int>(
                name: "validation_saga_step",
                table: "Drivers",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<string>(
                name: "validation_saga_reason",
                table: "Drivers",
                type: "character varying(500)",
                maxLength: 500,
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "validation_saga_started_at",
                table: "Drivers",
                type: "timestamp with time zone",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "created_at",
                table: "Drivers");

            migrationBuilder.DropColumn(
                name: "updated_at",
                table: "Drivers");

            migrationBuilder.DropColumn(
                name: "is_validation_completed",
                table: "Drivers");

            migrationBuilder.DropColumn(
                name: "validation_saga_step",
                table: "Drivers");

            migrationBuilder.DropColumn(
                name: "validation_saga_reason",
                table: "Drivers");

            migrationBuilder.DropColumn(
                name: "validation_saga_started_at",
                table: "Drivers");
        }
    }
}

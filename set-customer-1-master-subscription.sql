SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @CustomerId int = 1;
DECLARE @MasterModuleCode varchar(50) = 'MASTER-MODULE';
DECLARE @MasterPlanId int;
DECLARE @MasterBillingPeriodId int;
DECLARE @MasterModuleName nvarchar(255);
DECLARE @MasterPlanName nvarchar(255);
DECLARE @SubscriptionPlanName nvarchar(511);
DECLARE @CustomerModuleId int;
DECLARE @CurrentSubscriptionId int;

-- Safety guard: this script must never be broadened to other customers by accident.
IF @CustomerId <> 1
    THROW 50000, 'Safety check failed: this script may only update CustomerId = 1.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.CUSTOMER
    WHERE CustomerId = @CustomerId
)
    THROW 50001, 'CustomerId = 1 does not exist.', 1;

-- Select the newest currently valid MASTER plan. No plan ID is hard-coded.
SELECT TOP (1)
    @MasterPlanId = SP.SubscriptionPlanId,
    @MasterModuleName = SP.ModuleName,
    @MasterPlanName = SP.PlanName
FROM dbo.SUBSCRIPTION_PLAN SP
WHERE SP.ModuleCode = @MasterModuleCode
  AND SP.Active = 1
  AND SP.ValidFrom <= CONVERT(date, SYSUTCDATETIME())
  AND (SP.ValidTo IS NULL OR SP.ValidTo >= CONVERT(date, SYSUTCDATETIME()))
ORDER BY SP.ValidFrom DESC, SP.SubscriptionPlanId DESC;

IF @MasterPlanId IS NULL
    THROW 50002, 'No active and currently valid MASTER-MODULE subscription plan was found.', 1;

SELECT @SubscriptionPlanName =
    CASE
        WHEN NULLIF(LTRIM(RTRIM(@MasterModuleName)), '') IS NOT NULL
         AND NULLIF(LTRIM(RTRIM(@MasterPlanName)), '') IS NOT NULL
            THEN LTRIM(RTRIM(@MasterModuleName)) + ' - ' + LTRIM(RTRIM(@MasterPlanName))
        WHEN NULLIF(LTRIM(RTRIM(@MasterPlanName)), '') IS NOT NULL
            THEN LTRIM(RTRIM(@MasterPlanName))
        ELSE LTRIM(RTRIM(ISNULL(@MasterModuleName, 'Master Module')))
    END;

-- Prefer an active billing period matching the plan default; otherwise use the first active period.
SELECT TOP (1)
    @MasterBillingPeriodId = BP.SubscriptionPlanBillingPeriodId
FROM dbo.SUBSCRIPTION_PLAN_BILLING_PERIOD BP
INNER JOIN dbo.SUBSCRIPTION_PLAN SP
    ON SP.SubscriptionPlanId = BP.SubscriptionPlanId
WHERE BP.SubscriptionPlanId = @MasterPlanId
  AND BP.Active = 1
ORDER BY
    CASE WHEN BP.BillingPeriodMonths = SP.BillingPeriodMonths THEN 0 ELSE 1 END,
    BP.SubscriptionPlanBillingPeriodId;

BEGIN TRY
    BEGIN TRANSACTION;

    -- Keep one current module row. Older rows remain untouched as history.
    SELECT TOP (1)
        @CustomerModuleId = CM.CustomerModuleId
    FROM dbo.CUSTOMER_MODULE CM WITH (UPDLOCK, HOLDLOCK)
    WHERE CM.CustomerId = @CustomerId
      AND CM.Latest = 1
    ORDER BY CM.UpdatedAt DESC, CM.CustomerModuleId DESC;

    UPDATE dbo.CUSTOMER_MODULE
    SET Latest = 0,
        UpdatedAt = SYSUTCDATETIME()
    WHERE CustomerId = @CustomerId
      AND Latest = 1
      AND CustomerModuleId <> @CustomerModuleId;

    IF @CustomerModuleId IS NULL
    BEGIN
        INSERT INTO dbo.CUSTOMER_MODULE (
            CustomerId,
            SubscriptionPlanId,
            SubscriptionPlanBillingPeriodId,
            ModuleCode,
            ModuleName,
            CustomerModuleStatus,
            Latest
        )
        VALUES (
            @CustomerId,
            @MasterPlanId,
            @MasterBillingPeriodId,
            @MasterModuleCode,
            @MasterModuleName,
            1, -- ACTIVE
            1
        );
    END
    ELSE
    BEGIN
        UPDATE dbo.CUSTOMER_MODULE
        SET SubscriptionPlanId = @MasterPlanId,
            SubscriptionPlanBillingPeriodId = @MasterBillingPeriodId,
            ModuleCode = @MasterModuleCode,
            ModuleName = @MasterModuleName,
            CustomerModuleStatus = 1, -- ACTIVE
            Latest = 1,
            UpdatedAt = SYSUTCDATETIME()
        WHERE CustomerModuleId = @CustomerModuleId
          AND CustomerId = @CustomerId;
    END;

    -- Insert the missing subscription. On a rerun, keep the current row aligned with MASTER.
    SELECT TOP (1)
        @CurrentSubscriptionId = CS.SubscriptionId
    FROM dbo.CUSTOMER_SUBSCRIPTION CS WITH (UPDLOCK, HOLDLOCK)
    WHERE CS.CustomerId = @CustomerId
    ORDER BY CS.SubscriptionId DESC;

    IF @CurrentSubscriptionId IS NULL
    BEGIN
        INSERT INTO dbo.CUSTOMER_SUBSCRIPTION (
            CustomerId,
            SubscriptionStatus,
            SubscriptionPlanId,
            SubscriptionPlanBillingPeriodId,
            SubscriptionPlanName,
            TrialStartAt,
            TrialEndAt,
            TrialReminderSentAt,
            PeriodStartAt,
            PeriodEndAt,
            RenewalReminderSentAt,
            ContinuationConfirmedAt,
            RenewalConfirmedAt,
            GracePeriodEndsAt
        )
        VALUES (
            @CustomerId,
            'ACTIVE',
            @MasterPlanId,
            @MasterBillingPeriodId,
            @SubscriptionPlanName,
            NULL,
            NULL,
            NULL,
            SYSUTCDATETIME(),
            NULL, -- CustomerId 1 has no automatic expiry.
            NULL,
            NULL,
            NULL,
            NULL
        );
    END;
    ELSE
    BEGIN
        UPDATE dbo.CUSTOMER_SUBSCRIPTION
        SET SubscriptionStatus = 'ACTIVE',
            SubscriptionPlanId = @MasterPlanId,
            SubscriptionPlanBillingPeriodId = @MasterBillingPeriodId,
            SubscriptionPlanName = @SubscriptionPlanName,
            TrialStartAt = NULL,
            TrialEndAt = NULL,
            TrialReminderSentAt = NULL,
            PeriodEndAt = NULL,
            GracePeriodEndsAt = NULL,
            UpdatedAt = SYSUTCDATETIME()
        WHERE SubscriptionId = @CurrentSubscriptionId
          AND CustomerId = @CustomerId;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;

-- Verification: both result sets are deliberately restricted to CustomerId = 1.
SELECT
    CM.CustomerModuleId,
    CM.CustomerId,
    CM.SubscriptionPlanId,
    CM.SubscriptionPlanBillingPeriodId,
    CM.ModuleCode,
    CM.ModuleName,
    CM.CustomerModuleStatus,
    CM.Latest
FROM dbo.CUSTOMER_MODULE CM
WHERE CM.CustomerId = 1
ORDER BY CM.Latest DESC, CM.CustomerModuleId DESC;

SELECT
    CS.SubscriptionId,
    CS.CustomerId,
    CS.SubscriptionStatus,
    CS.SubscriptionPlanId,
    CS.SubscriptionPlanBillingPeriodId,
    CS.SubscriptionPlanName,
    CS.PeriodStartAt,
    CS.PeriodEndAt
FROM dbo.CUSTOMER_SUBSCRIPTION CS
WHERE CS.CustomerId = 1
ORDER BY CS.SubscriptionId DESC;

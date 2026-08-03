/*
    Adds SubscriptionPlanBillingPeriodId to [dbo].[CUSTOMER_MODULE].
*/

IF COL_LENGTH('dbo.CUSTOMER_MODULE', 'SubscriptionPlanBillingPeriodId') IS NULL
BEGIN
ALTER TABLE [dbo].[CUSTOMER_MODULE]
    ADD [SubscriptionPlanBillingPeriodId] [int] NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_CUSTOMER_MODULE_SubscriptionPlanBillingPeriod'
      AND parent_object_id = OBJECT_ID(N'[dbo].[CUSTOMER_MODULE]')
)
BEGIN
ALTER TABLE [dbo].[CUSTOMER_MODULE] WITH CHECK
    ADD CONSTRAINT [FK_CUSTOMER_MODULE_SubscriptionPlanBillingPeriod]
    FOREIGN KEY ([SubscriptionPlanBillingPeriodId])
    REFERENCES [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD]([SubscriptionPlanBillingPeriodId]);
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_MODULE_BillingPeriod_Latest'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_MODULE]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_MODULE_BillingPeriod_Latest]
    ON [dbo].[CUSTOMER_MODULE] ([SubscriptionPlanBillingPeriodId], [Latest])
    INCLUDE ([CustomerId], [SubscriptionPlanId], [ModuleCode], [CustomerModuleStatus]);
END
GO

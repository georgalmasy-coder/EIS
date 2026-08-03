/*
    Adds SubscriptionPlanBillingPeriodId to [dbo].[CUSTOMER_SUBSCRIPTION].
*/

IF COL_LENGTH('dbo.CUSTOMER_SUBSCRIPTION', 'SubscriptionPlanBillingPeriodId') IS NULL
BEGIN
ALTER TABLE [dbo].[CUSTOMER_SUBSCRIPTION]
    ADD [SubscriptionPlanBillingPeriodId] [int] NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_CUSTOMER_SUBSCRIPTION_SubscriptionPlanBillingPeriod'
      AND parent_object_id = OBJECT_ID(N'[dbo].[CUSTOMER_SUBSCRIPTION]')
)
BEGIN
ALTER TABLE [dbo].[CUSTOMER_SUBSCRIPTION] WITH CHECK
    ADD CONSTRAINT [FK_CUSTOMER_SUBSCRIPTION_SubscriptionPlanBillingPeriod]
    FOREIGN KEY ([SubscriptionPlanBillingPeriodId])
    REFERENCES [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD]([SubscriptionPlanBillingPeriodId]);
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_CUSTOMER_SUBSCRIPTION_BillingPeriod'
      AND object_id = OBJECT_ID(N'[dbo].[CUSTOMER_SUBSCRIPTION]')
)
BEGIN
CREATE INDEX [IX_CUSTOMER_SUBSCRIPTION_BillingPeriod]
    ON [dbo].[CUSTOMER_SUBSCRIPTION] ([SubscriptionPlanBillingPeriodId]);
END
GO

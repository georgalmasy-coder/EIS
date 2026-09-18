package dk.eis.tech.timesheet.data;

import dk.eis.tech.timesheet.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class AccountingRepository {

    public List<AccountRecord> findAccounts(boolean activeOnly) throws SQLException {
        String sql = """
                SELECT id, account_number, account_name, account_type, system_key, is_active, created_at, updated_at
                FROM dbo.accounts
                WHERE (? = 0 OR is_active = 1)
                ORDER BY account_number
                """;
        List<AccountRecord> accounts = new ArrayList<>();
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, activeOnly);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) accounts.add(mapAccount(rs));
            }
        }
        return accounts;
    }

    public long insertAccount(AccountUpsertRequest request) throws SQLException {
        String sql = """
                INSERT INTO dbo.accounts (account_number, account_name, account_type, is_active)
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAccount(statement, request);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        throw new SQLException("Unable to create account");
    }

    public void updateAccount(long id, AccountUpsertRequest request) throws SQLException {
        String sql = """
                UPDATE dbo.accounts
                SET account_number = ?, account_name = ?, account_type = ?, is_active = ?, updated_at = SYSDATETIME()
                WHERE id = ?
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAccount(statement, request);
            statement.setLong(5, id);
            statement.executeUpdate();
        }
    }

    public Map<String, AccountRecord> systemAccounts(Connection connection) throws SQLException {
        Map<String, AccountRecord> accounts = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, account_number, account_name, account_type, system_key, is_active, created_at, updated_at
                FROM dbo.accounts
                WHERE system_key IS NOT NULL
                """);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                AccountRecord account = mapAccount(rs);
                accounts.put(account.systemKey(), account);
            }
        }
        return accounts;
    }

    public List<AccountingEntryRecord> findEntries(int year, Integer month) throws SQLException {
        return findEntries(year, month, month);
    }

    public List<AccountingEntryRecord> findEntries(int year, Integer fromMonth, Integer toMonth) throws SQLException {
        String sql = """
                SELECT id, entry_date, accounting_year, accounting_month, entry_type, description,
                       reference_type, reference_id, correction_of_entry_id, created_at, updated_at
                FROM dbo.accounting_entries
                WHERE accounting_year = ?
                  AND (? IS NULL OR accounting_month >= ?)
                  AND (? IS NULL OR accounting_month <= ?)
                ORDER BY entry_date DESC, id DESC
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (fromMonth == null) {
                statement.setNull(2, Types.INTEGER);
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(2, fromMonth);
                statement.setInt(3, fromMonth);
            }
            if (toMonth == null) {
                statement.setNull(4, Types.INTEGER);
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(4, toMonth);
                statement.setInt(5, toMonth);
            }
            List<AccountingEntryRecord> entries = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) entries.add(mapEntry(rs, List.of()));
            }
            return withLines(connection, entries);
        }
    }

    public Optional<AccountingEntryRecord> findEntry(long id) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, entry_date, accounting_year, accounting_month, entry_type, description,
                            reference_type, reference_id, correction_of_entry_id, created_at, updated_at
                     FROM dbo.accounting_entries
                     WHERE id = ?
                     """)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return Optional.of(withLines(connection, List.of(mapEntry(rs, List.of()))).getFirst());
            }
        }
        return Optional.empty();
    }

    public long insertEntry(AccountingEntryRequest request) throws SQLException {
        validateBalanced(request.lines());
        LocalDate entryDate = requireDate(request.entryDate());
        try (Connection connection = Database.connection()) {
            connection.setAutoCommit(false);
            try {
                long entryId = insertEntryHeader(connection, entryDate, request.entryType(), request.description(),
                        request.referenceType(), request.referenceId(), request.correctionOfEntryId());
                insertLines(connection, entryId, request.lines());
                connection.commit();
                return entryId;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public long correctEntry(long originalEntryId, AccountingEntryRequest replacement) throws SQLException {
        AccountingEntryRecord original = findEntry(originalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found"));
        validateBalanced(replacement.lines());
        LocalDate entryDate = requireDate(replacement.entryDate());
        try (Connection connection = Database.connection()) {
            connection.setAutoCommit(false);
            try {
                List<AccountingLineRequest> reversal = original.lines().stream()
                        .map(line -> new AccountingLineRequest(
                                line.accountId(),
                                "Correction of entry " + original.id() + ": " + line.lineText(),
                                line.creditAmount(),
                                line.debitAmount(),
                                line.vatCode()))
                        .toList();
                long reversalId = insertEntryHeader(connection, entryDate, "CORRECTION_REVERSAL",
                        "Correction reversal for entry " + original.id(), original.referenceType(),
                        original.referenceId(), original.id());
                insertLines(connection, reversalId, reversal);
                long replacementId = insertEntryHeader(connection, entryDate, replacement.entryType(), replacement.description(),
                        replacement.referenceType(), replacement.referenceId(), original.id());
                insertLines(connection, replacementId, replacement.lines());
                connection.commit();
                return replacementId;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Optional<InvoiceApprovalRecord> findInvoiceApproval(long customerId, int year, int month) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, customer_id, invoice_year, invoice_month, invoice_number, invoice_date,
                            subtotal, vat_amount, total, accounting_entry_id, created_at
                     FROM dbo.invoice_approvals
                     WHERE customer_id = ? AND invoice_year = ? AND invoice_month = ?
                     """)) {
            statement.setLong(1, customerId);
            statement.setInt(2, year);
            statement.setInt(3, month);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return Optional.of(mapInvoiceApproval(rs));
            }
        }
        return Optional.empty();
    }

    public InvoiceApprovalRecord approveInvoice(long customerId, int year, int month, String invoiceNumber,
                                                LocalDate invoiceDate, BigDecimal subtotal,
                                                BigDecimal vatAmount, BigDecimal total) throws SQLException {
        try (Connection connection = Database.connection()) {
            connection.setAutoCommit(false);
            try {
                if (invoiceApprovalExists(connection, customerId, year, month)) {
                    throw new IllegalArgumentException("Invoice is already approved");
                }
                Map<String, AccountRecord> accounts = systemAccounts(connection);
                AccountingEntryRequest entry = invoiceEntry(accounts, invoiceNumber, invoiceDate, subtotal, vatAmount, total);
                long entryId = insertEntryHeader(connection, invoiceDate, entry.entryType(), entry.description(),
                        entry.referenceType(), entry.referenceId(), null);
                insertLines(connection, entryId, entry.lines());
                long approvalId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO dbo.invoice_approvals (
                            customer_id, invoice_year, invoice_month, invoice_number, invoice_date,
                            subtotal, vat_amount, total, accounting_entry_id
                        )
                        OUTPUT INSERTED.id
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setLong(1, customerId);
                    statement.setInt(2, year);
                    statement.setInt(3, month);
                    statement.setString(4, invoiceNumber);
                    statement.setDate(5, java.sql.Date.valueOf(invoiceDate));
                    statement.setBigDecimal(6, money(subtotal));
                    statement.setBigDecimal(7, money(vatAmount));
                    statement.setBigDecimal(8, money(total));
                    statement.setLong(9, entryId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Unable to approve invoice");
                        approvalId = rs.getLong("id");
                    }
                }
                connection.commit();
                return new InvoiceApprovalRecord(approvalId, customerId, year, month, invoiceNumber, invoiceDate,
                        money(subtotal), money(vatAmount), money(total), entryId, null);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public InvoicePostingPreview invoicePreview(String invoiceNumber, LocalDate invoiceDate, BigDecimal subtotal,
                                                BigDecimal vatAmount, BigDecimal total) throws SQLException {
        try (Connection connection = Database.connection()) {
            AccountingEntryRequest entry = invoiceEntry(systemAccounts(connection), invoiceNumber, invoiceDate, subtotal, vatAmount, total);
            List<AccountingLineRecord> lines = new ArrayList<>();
            long lineId = 1;
            for (AccountingLineRequest line : entry.lines()) {
                AccountRecord account = findAccountById(connection, line.accountId());
                lines.add(new AccountingLineRecord(lineId++, 0, account.id(), account.accountNumber(), account.accountName(),
                        line.lineText(), money(line.debitAmount()), money(line.creditAmount()), line.vatCode()));
            }
            return new InvoicePostingPreview(invoiceNumber, null, lines);
        }
    }

    private AccountingEntryRequest invoiceEntry(Map<String, AccountRecord> accounts, String invoiceNumber, LocalDate invoiceDate,
                                                BigDecimal subtotal, BigDecimal vatAmount, BigDecimal total) {
        AccountRecord receivable = requiredSystemAccount(accounts, "ACCOUNTS_RECEIVABLE");
        AccountRecord revenue = requiredSystemAccount(accounts, "REVENUE");
        AccountRecord outputVat = requiredSystemAccount(accounts, "OUTPUT_VAT");
        return new AccountingEntryRequest(invoiceDate, "CUSTOMER_INVOICE", "Invoice " + invoiceNumber,
                "INVOICE", invoiceNumber, null, List.of(
                new AccountingLineRequest(receivable.id(), "Invoice " + invoiceNumber, total, BigDecimal.ZERO, null),
                new AccountingLineRequest(revenue.id(), "Invoice " + invoiceNumber + " revenue", BigDecimal.ZERO, subtotal, null),
                new AccountingLineRequest(outputVat.id(), "Invoice " + invoiceNumber + " VAT", BigDecimal.ZERO, vatAmount, "OUTPUT_VAT")
        ));
    }

    private long insertEntryHeader(Connection connection, LocalDate date, String type, String description,
                                   String referenceType, String referenceId, Long correctionOfEntryId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dbo.accounting_entries (
                    entry_date, accounting_year, accounting_month, entry_type, description,
                    reference_type, reference_id, correction_of_entry_id
                )
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setDate(1, java.sql.Date.valueOf(date));
            statement.setInt(2, date.getYear());
            statement.setInt(3, date.getMonthValue());
            statement.setString(4, requiredText(type, "Entry type"));
            statement.setString(5, requiredText(description, "Description"));
            statement.setString(6, optionalText(referenceType));
            statement.setString(7, optionalText(referenceId));
            if (correctionOfEntryId == null) statement.setNull(8, Types.BIGINT); else statement.setLong(8, correctionOfEntryId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        throw new SQLException("Unable to create accounting entry");
    }

    private void insertLines(Connection connection, long entryId, List<AccountingLineRequest> lines) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dbo.accounting_lines (entry_id, account_id, line_text, debit_amount, credit_amount, vat_code)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (AccountingLineRequest line : lines) {
                statement.setLong(1, entryId);
                statement.setLong(2, requireId(line.accountId(), "Account"));
                statement.setString(3, requiredText(line.lineText(), "Line text"));
                statement.setBigDecimal(4, money(line.debitAmount()));
                statement.setBigDecimal(5, money(line.creditAmount()));
                statement.setString(6, optionalText(line.vatCode()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<AccountingEntryRecord> withLines(Connection connection, List<AccountingEntryRecord> entries) throws SQLException {
        if (entries.isEmpty()) return entries;
        Map<Long, List<AccountingLineRecord>> linesByEntry = new HashMap<>();
        String placeholders = String.join(",", Collections.nCopies(entries.size(), "?"));
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT l.id, l.entry_id, l.account_id, a.account_number, a.account_name,
                       l.line_text, l.debit_amount, l.credit_amount, l.vat_code
                FROM dbo.accounting_lines l
                JOIN dbo.accounts a ON a.id = l.account_id
                WHERE l.entry_id IN (
                """ + placeholders + """
                )
                ORDER BY l.entry_id, l.id
                """)) {
            for (int i = 0; i < entries.size(); i++) statement.setLong(i + 1, entries.get(i).id());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    AccountingLineRecord line = mapLine(rs);
                    linesByEntry.computeIfAbsent(line.entryId(), key -> new ArrayList<>()).add(line);
                }
            }
        }
        return entries.stream()
                .map(entry -> new AccountingEntryRecord(entry.id(), entry.entryDate(), entry.accountingYear(),
                        entry.accountingMonth(), entry.entryType(), entry.description(), entry.referenceType(),
                        entry.referenceId(), entry.correctionOfEntryId(), entry.createdAt(), entry.updatedAt(),
                        linesByEntry.getOrDefault(entry.id(), List.of())))
                .toList();
    }

    private boolean invoiceApprovalExists(Connection connection, long customerId, int year, int month) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM dbo.invoice_approvals
                WHERE customer_id = ? AND invoice_year = ? AND invoice_month = ?
                """)) {
            statement.setLong(1, customerId);
            statement.setInt(2, year);
            statement.setInt(3, month);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private AccountRecord findAccountById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, account_number, account_name, account_type, system_key, is_active, created_at, updated_at
                FROM dbo.accounts
                WHERE id = ?
                """)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        }
        throw new IllegalArgumentException("Account not found");
    }

    private void validateBalanced(List<AccountingLineRequest> lines) {
        if (lines == null || lines.size() < 2) throw new IllegalArgumentException("At least two accounting lines are required");
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (AccountingLineRequest line : lines) {
            BigDecimal lineDebit = money(line.debitAmount());
            BigDecimal lineCredit = money(line.creditAmount());
            if ((lineDebit.signum() > 0 && lineCredit.signum() > 0) || (lineDebit.signum() == 0 && lineCredit.signum() == 0)) {
                throw new IllegalArgumentException("Each line must have either debit or credit amount");
            }
            debit = debit.add(lineDebit);
            credit = credit.add(lineCredit);
        }
        if (debit.compareTo(credit) != 0) throw new IllegalArgumentException("Debit and credit must balance");
    }

    private void bindAccount(PreparedStatement statement, AccountUpsertRequest request) throws SQLException {
        statement.setString(1, requiredText(request.accountNumber(), "Account number"));
        statement.setString(2, requiredText(request.accountName(), "Account name"));
        statement.setString(3, requiredText(request.accountType(), "Account type"));
        statement.setBoolean(4, request.active());
    }

    private AccountRecord mapAccount(ResultSet rs) throws SQLException {
        return new AccountRecord(rs.getLong("id"), rs.getString("account_number"), rs.getString("account_name"),
                rs.getString("account_type"), rs.getString("system_key"), rs.getBoolean("is_active"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private AccountingEntryRecord mapEntry(ResultSet rs, List<AccountingLineRecord> lines) throws SQLException {
        Long correctionId = rs.getObject("correction_of_entry_id") == null ? null : rs.getLong("correction_of_entry_id");
        return new AccountingEntryRecord(rs.getLong("id"), rs.getDate("entry_date").toLocalDate(),
                rs.getInt("accounting_year"), rs.getInt("accounting_month"), rs.getString("entry_type"),
                rs.getString("description"), rs.getString("reference_type"), rs.getString("reference_id"),
                correctionId, rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime(),
                lines);
    }

    private AccountingLineRecord mapLine(ResultSet rs) throws SQLException {
        return new AccountingLineRecord(rs.getLong("id"), rs.getLong("entry_id"), rs.getLong("account_id"),
                rs.getString("account_number"), rs.getString("account_name"), rs.getString("line_text"),
                rs.getBigDecimal("debit_amount"), rs.getBigDecimal("credit_amount"), rs.getString("vat_code"));
    }

    private InvoiceApprovalRecord mapInvoiceApproval(ResultSet rs) throws SQLException {
        return new InvoiceApprovalRecord(rs.getLong("id"), rs.getLong("customer_id"), rs.getInt("invoice_year"),
                rs.getInt("invoice_month"), rs.getString("invoice_number"), rs.getDate("invoice_date").toLocalDate(),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("vat_amount"), rs.getBigDecimal("total"),
                rs.getLong("accounting_entry_id"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private AccountRecord requiredSystemAccount(Map<String, AccountRecord> accounts, String key) {
        AccountRecord account = accounts.get(key);
        if (account == null || !account.active()) throw new IllegalArgumentException("Required account is missing or inactive: " + key);
        return account;
    }

    private long requireId(Long value, String label) {
        if (value == null || value <= 0) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private LocalDate requireDate(LocalDate value) {
        if (value == null) throw new IllegalArgumentException("Entry date is required");
        return value;
    }

    private String requiredText(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

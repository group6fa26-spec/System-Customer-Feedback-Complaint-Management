SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (SELECT 1 FROM feedbacks WHERE customer_id IS NULL)
        THROW 51000, 'Cannot add feedbacks.customer_id NOT NULL: NULL values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks WHERE status IS NULL)
        THROW 51001, 'Cannot add feedbacks.status NOT NULL: NULL values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks WHERE created_at IS NULL)
        THROW 51002, 'Cannot add feedbacks.created_at NOT NULL: NULL values exist.', 1;
    IF EXISTS (SELECT 1 FROM satisfaction_ratings WHERE stars NOT BETWEEN 1 AND 5)
        THROW 51003, 'Cannot add rating stars check: invalid values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks WHERE duplicate_of_id = id)
        THROW 51004, 'Cannot add duplicate self-reference check: self-duplicate rows exist.', 1;
    IF EXISTS (SELECT email FROM customers WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1)
        THROW 51005, 'Cannot add unique customer email index: duplicates exist.', 1;
    IF EXISTS (SELECT phone FROM customers WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*) > 1)
        THROW 51006, 'Cannot add unique customer phone index: duplicates exist.', 1;
    IF EXISTS (SELECT customer_id FROM user_accounts WHERE customer_id IS NOT NULL GROUP BY customer_id HAVING COUNT(*) > 1)
        THROW 51007, 'Cannot add unique user_accounts.customer_id index: duplicates exist.', 1;
    IF EXISTS (SELECT staff_id FROM user_accounts WHERE staff_id IS NOT NULL GROUP BY staff_id HAVING COUNT(*) > 1)
        THROW 51008, 'Cannot add unique user_accounts.staff_id index: duplicates exist.', 1;

    IF EXISTS (SELECT 1 FROM feedbacks f LEFT JOIN customers c ON c.id = f.customer_id WHERE c.id IS NULL)
        THROW 51009, 'Orphan feedbacks.customer_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks f LEFT JOIN feedback_categories c ON c.id = f.category_id WHERE f.category_id IS NOT NULL AND c.id IS NULL)
        THROW 51010, 'Orphan feedbacks.category_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks f LEFT JOIN feedback_categories c ON c.id = f.auto_category_id WHERE f.auto_category_id IS NOT NULL AND c.id IS NULL)
        THROW 51011, 'Orphan feedbacks.auto_category_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks f LEFT JOIN staff s ON s.id = f.about_staff_id WHERE f.about_staff_id IS NOT NULL AND s.id IS NULL)
        THROW 51012, 'Orphan feedbacks.about_staff_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks f LEFT JOIN staff s ON s.id = f.assigned_staff_id WHERE f.assigned_staff_id IS NOT NULL AND s.id IS NULL)
        THROW 51013, 'Orphan feedbacks.assigned_staff_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks f LEFT JOIN staff s ON s.id = f.approved_by_manager_id WHERE f.approved_by_manager_id IS NOT NULL AND s.id IS NULL)
        THROW 51014, 'Orphan feedbacks.approved_by_manager_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedbacks f LEFT JOIN feedbacks original ON original.id = f.duplicate_of_id WHERE f.duplicate_of_id IS NOT NULL AND original.id IS NULL)
        THROW 51015, 'Orphan feedbacks.duplicate_of_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedback_assignments a LEFT JOIN feedbacks f ON f.id = a.feedback_id WHERE a.feedback_id IS NOT NULL AND f.id IS NULL)
        THROW 51016, 'Orphan feedback_assignments.feedback_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedback_assignments a LEFT JOIN staff s ON s.id = a.staff_id WHERE a.staff_id IS NOT NULL AND s.id IS NULL)
        THROW 51017, 'Orphan feedback_assignments.staff_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedback_assignments a LEFT JOIN staff s ON s.id = a.assigned_by_id WHERE a.assigned_by_id IS NOT NULL AND s.id IS NULL)
        THROW 51018, 'Orphan feedback_assignments.assigned_by_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedback_attachments a LEFT JOIN feedbacks f ON f.id = a.feedback_id WHERE a.feedback_id IS NOT NULL AND f.id IS NULL)
        THROW 51019, 'Orphan feedback_attachments.feedback_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedback_responses r LEFT JOIN feedbacks f ON f.id = r.feedback_id WHERE r.feedback_id IS NOT NULL AND f.id IS NULL)
        THROW 51020, 'Orphan feedback_responses.feedback_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM escalation_histories e LEFT JOIN feedbacks f ON f.id = e.feedback_id WHERE e.feedback_id IS NOT NULL AND f.id IS NULL)
        THROW 51021, 'Orphan escalation_histories.feedback_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM escalation_histories e LEFT JOIN staff s ON s.id = e.from_staff_id WHERE e.from_staff_id IS NOT NULL AND s.id IS NULL)
        THROW 51022, 'Orphan escalation_histories.from_staff_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM escalation_histories e LEFT JOIN staff s ON s.id = e.to_manager_id WHERE e.to_manager_id IS NOT NULL AND s.id IS NULL)
        THROW 51023, 'Orphan escalation_histories.to_manager_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM status_histories h LEFT JOIN feedbacks f ON f.id = h.feedback_id WHERE h.feedback_id IS NOT NULL AND f.id IS NULL)
        THROW 51024, 'Orphan status_histories.feedback_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM satisfaction_ratings r LEFT JOIN feedbacks f ON f.id = r.feedback_id WHERE r.feedback_id IS NOT NULL AND f.id IS NULL)
        THROW 51025, 'Orphan satisfaction_ratings.feedback_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM satisfaction_ratings r LEFT JOIN customers c ON c.id = r.customer_id WHERE r.customer_id IS NOT NULL AND c.id IS NULL)
        THROW 51026, 'Orphan satisfaction_ratings.customer_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM user_accounts a LEFT JOIN customers c ON c.id = a.customer_id WHERE a.customer_id IS NOT NULL AND c.id IS NULL)
        THROW 51027, 'Orphan user_accounts.customer_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM user_accounts a LEFT JOIN staff s ON s.id = a.staff_id WHERE a.staff_id IS NOT NULL AND s.id IS NULL)
        THROW 51028, 'Orphan user_accounts.staff_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM feedback_category_keywords k LEFT JOIN feedback_categories c ON c.id = k.category_id WHERE c.id IS NULL)
        THROW 51029, 'Orphan feedback_category_keywords.category_id values exist.', 1;
    IF EXISTS (SELECT 1 FROM rental_contracts r LEFT JOIN customers c ON c.id = r.tenant_customer_id WHERE c.id IS NULL)
        THROW 51030, 'Orphan rental_contracts.tenant_customer_id values exist.', 1;

    ALTER TABLE feedbacks ALTER COLUMN customer_id BIGINT NOT NULL;
    ALTER TABLE feedbacks ALTER COLUMN status NVARCHAR(255) NOT NULL;
    ALTER TABLE feedbacks ALTER COLUMN created_at DATETIME2(7) NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_satisfaction_ratings_stars_range')
        ALTER TABLE satisfaction_ratings WITH CHECK ADD CONSTRAINT CK_satisfaction_ratings_stars_range CHECK (stars BETWEEN 1 AND 5);
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_feedbacks_not_self_duplicate')
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT CK_feedbacks_not_self_duplicate CHECK (duplicate_of_id IS NULL OR duplicate_of_id <> id);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('customers') AND name = 'UX_customers_email')
        CREATE UNIQUE INDEX UX_customers_email ON customers(email) WHERE email IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('customers') AND name = 'UX_customers_phone')
        CREATE UNIQUE INDEX UX_customers_phone ON customers(phone) WHERE phone IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('user_accounts') AND name = 'UX_user_accounts_customer_id')
        CREATE UNIQUE INDEX UX_user_accounts_customer_id ON user_accounts(customer_id) WHERE customer_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('user_accounts') AND name = 'UX_user_accounts_staff_id')
        CREATE UNIQUE INDEX UX_user_accounts_staff_id ON user_accounts(staff_id) WHERE staff_id IS NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedbacks') AND name = 'IX_feedbacks_customer_created_at')
        CREATE INDEX IX_feedbacks_customer_created_at ON feedbacks(customer_id, created_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedbacks') AND name = 'IX_feedbacks_category_id')
        CREATE INDEX IX_feedbacks_category_id ON feedbacks(category_id) WHERE category_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedbacks') AND name = 'IX_feedbacks_auto_category_id')
        CREATE INDEX IX_feedbacks_auto_category_id ON feedbacks(auto_category_id) WHERE auto_category_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedbacks') AND name = 'IX_feedbacks_about_staff_id')
        CREATE INDEX IX_feedbacks_about_staff_id ON feedbacks(about_staff_id) WHERE about_staff_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedbacks') AND name = 'IX_feedbacks_assigned_staff_id')
        CREATE INDEX IX_feedbacks_assigned_staff_id ON feedbacks(assigned_staff_id) WHERE assigned_staff_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedbacks') AND name = 'IX_feedbacks_approved_by_manager_id')
        CREATE INDEX IX_feedbacks_approved_by_manager_id ON feedbacks(approved_by_manager_id) WHERE approved_by_manager_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedbacks') AND name = 'IX_feedbacks_duplicate_of_id')
        CREATE INDEX IX_feedbacks_duplicate_of_id ON feedbacks(duplicate_of_id) WHERE duplicate_of_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedback_assignments') AND name = 'IX_feedback_assignments_feedback_id')
        CREATE INDEX IX_feedback_assignments_feedback_id ON feedback_assignments(feedback_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedback_assignments') AND name = 'IX_feedback_assignments_staff_id')
        CREATE INDEX IX_feedback_assignments_staff_id ON feedback_assignments(staff_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedback_assignments') AND name = 'IX_feedback_assignments_assigned_by_id')
        CREATE INDEX IX_feedback_assignments_assigned_by_id ON feedback_assignments(assigned_by_id) WHERE assigned_by_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedback_attachments') AND name = 'IX_feedback_attachments_feedback_id')
        CREATE INDEX IX_feedback_attachments_feedback_id ON feedback_attachments(feedback_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('feedback_responses') AND name = 'IX_feedback_responses_feedback_id')
        CREATE INDEX IX_feedback_responses_feedback_id ON feedback_responses(feedback_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('escalation_histories') AND name = 'IX_escalation_histories_feedback_id')
        CREATE INDEX IX_escalation_histories_feedback_id ON escalation_histories(feedback_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('status_histories') AND name = 'IX_status_histories_feedback_id')
        CREATE INDEX IX_status_histories_feedback_id ON status_histories(feedback_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('satisfaction_ratings') AND name = 'IX_satisfaction_ratings_customer_id')
        CREATE INDEX IX_satisfaction_ratings_customer_id ON satisfaction_ratings(customer_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('rental_contracts') AND name = 'IX_rental_contracts_tenant_start_date')
        CREATE INDEX IX_rental_contracts_tenant_start_date ON rental_contracts(tenant_customer_id, start_date DESC);

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedbacks') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedbacks'), 'customer_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('customers'))
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT FK_feedbacks_customer FOREIGN KEY (customer_id) REFERENCES customers(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedbacks') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedbacks'), 'category_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedback_categories'))
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT FK_feedbacks_category FOREIGN KEY (category_id) REFERENCES feedback_categories(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedbacks') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedbacks'), 'auto_category_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedback_categories'))
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT FK_feedbacks_auto_category FOREIGN KEY (auto_category_id) REFERENCES feedback_categories(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedbacks') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedbacks'), 'about_staff_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT FK_feedbacks_about_staff FOREIGN KEY (about_staff_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedbacks') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedbacks'), 'assigned_staff_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT FK_feedbacks_assigned_staff FOREIGN KEY (assigned_staff_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedbacks') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedbacks'), 'approved_by_manager_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT FK_feedbacks_approved_by_manager FOREIGN KEY (approved_by_manager_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedbacks') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedbacks'), 'duplicate_of_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedbacks'))
        ALTER TABLE feedbacks WITH CHECK ADD CONSTRAINT FK_feedbacks_duplicate_of FOREIGN KEY (duplicate_of_id) REFERENCES feedbacks(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedback_assignments') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedback_assignments'), 'feedback_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedbacks'))
        ALTER TABLE feedback_assignments WITH CHECK ADD CONSTRAINT FK_feedback_assignments_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedback_assignments') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedback_assignments'), 'staff_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE feedback_assignments WITH CHECK ADD CONSTRAINT FK_feedback_assignments_staff FOREIGN KEY (staff_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedback_assignments') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedback_assignments'), 'assigned_by_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE feedback_assignments WITH CHECK ADD CONSTRAINT FK_feedback_assignments_assigned_by FOREIGN KEY (assigned_by_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedback_attachments') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedback_attachments'), 'feedback_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedbacks'))
        ALTER TABLE feedback_attachments WITH CHECK ADD CONSTRAINT FK_feedback_attachments_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedback_responses') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedback_responses'), 'feedback_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedbacks'))
        ALTER TABLE feedback_responses WITH CHECK ADD CONSTRAINT FK_feedback_responses_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('escalation_histories') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('escalation_histories'), 'feedback_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedbacks'))
        ALTER TABLE escalation_histories WITH CHECK ADD CONSTRAINT FK_escalation_histories_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('escalation_histories') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('escalation_histories'), 'from_staff_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE escalation_histories WITH CHECK ADD CONSTRAINT FK_escalation_histories_from_staff FOREIGN KEY (from_staff_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('escalation_histories') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('escalation_histories'), 'to_manager_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE escalation_histories WITH CHECK ADD CONSTRAINT FK_escalation_histories_to_manager FOREIGN KEY (to_manager_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('status_histories') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('status_histories'), 'feedback_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedbacks'))
        ALTER TABLE status_histories WITH CHECK ADD CONSTRAINT FK_status_histories_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('satisfaction_ratings') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('satisfaction_ratings'), 'feedback_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedbacks'))
        ALTER TABLE satisfaction_ratings WITH CHECK ADD CONSTRAINT FK_satisfaction_ratings_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('satisfaction_ratings') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('satisfaction_ratings'), 'customer_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('customers'))
        ALTER TABLE satisfaction_ratings WITH CHECK ADD CONSTRAINT FK_satisfaction_ratings_customer FOREIGN KEY (customer_id) REFERENCES customers(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('user_accounts') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('user_accounts'), 'customer_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('customers'))
        ALTER TABLE user_accounts WITH CHECK ADD CONSTRAINT FK_user_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('user_accounts') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('user_accounts'), 'staff_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('staff'))
        ALTER TABLE user_accounts WITH CHECK ADD CONSTRAINT FK_user_accounts_staff FOREIGN KEY (staff_id) REFERENCES staff(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('feedback_category_keywords') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('feedback_category_keywords'), 'category_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('feedback_categories'))
        ALTER TABLE feedback_category_keywords WITH CHECK ADD CONSTRAINT FK_feedback_category_keywords_category FOREIGN KEY (category_id) REFERENCES feedback_categories(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id WHERE fk.parent_object_id = OBJECT_ID('rental_contracts') AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID('rental_contracts'), 'tenant_customer_id', 'ColumnId') AND fkc.referenced_object_id = OBJECT_ID('customers'))
        ALTER TABLE rental_contracts WITH CHECK ADD CONSTRAINT FK_rental_contracts_tenant_customer FOREIGN KEY (tenant_customer_id) REFERENCES customers(id);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

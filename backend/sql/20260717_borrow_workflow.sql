USE lab_equipment;

ALTER TABLE borrow_record
  MODIFY COLUMN borrow_time DATETIME NULL,
  MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '待审核',
  ADD COLUMN borrower_id INT NOT NULL AFTER device_id,
  ADD COLUMN purpose VARCHAR(500) NOT NULL AFTER borrower_id,
  ADD COLUMN approved_by INT NULL AFTER operator_id,
  ADD COLUMN approved_at DATETIME NULL AFTER approved_by,
  ADD COLUMN rejected_by INT NULL AFTER approved_at,
  ADD COLUMN rejected_at DATETIME NULL AFTER rejected_by,
  ADD COLUMN reject_reason VARCHAR(500) NULL AFTER rejected_at,
  ADD COLUMN return_requested_at DATETIME NULL AFTER reject_reason,
  ADD COLUMN return_confirmed_by INT NULL AFTER return_requested_at,
  ADD COLUMN return_confirmed_at DATETIME NULL AFTER return_confirmed_by,
  ADD COLUMN return_check_note TEXT NULL AFTER return_confirmed_at,
  ADD COLUMN return_device_status VARCHAR(20) NULL AFTER return_check_note;

ALTER TABLE borrow_record
  ADD KEY idx_borrow_borrower_status (borrower_id, status),
  ADD KEY idx_borrow_status_expected (status, expected_return_time),
  ADD KEY idx_borrow_approved_by (approved_by),
  ADD KEY idx_borrow_return_confirmed_by (return_confirmed_by),
  ADD CONSTRAINT fk_borrow_borrower FOREIGN KEY (borrower_id) REFERENCES user(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_borrow_approved_by FOREIGN KEY (approved_by) REFERENCES user(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_borrow_rejected_by FOREIGN KEY (rejected_by) REFERENCES user(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_borrow_return_confirmed_by FOREIGN KEY (return_confirmed_by) REFERENCES user(id) ON DELETE SET NULL;

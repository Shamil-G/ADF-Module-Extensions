package sharedApp.extensions;

import oracle.jbo.Key;
import oracle.jbo.Row;
import oracle.jbo.server.TransactionEvent;
import oracle.jbo.server.ViewObjectImpl;
import oracle.jbo.server.ViewRowImpl;

public class ExtensionViewObjectImpl extends ViewObjectImpl {

    private Key currentRowKeyBeforeRollback;

    @Override
    protected void create() {
        super.create();
        // allow read-only View objects to use findByKey() methods
        this.setManageRowsByKey(true);
    }


    @Override
    public void beforeRollback(TransactionEvent transactionEvent) {
        super.beforeRollback(transactionEvent);
        // check for query execution
        if (isExecuted()) {
            ExtensionViewRowImpl currentRow =
                (ExtensionViewRowImpl)getCurrentRow();
            if (currentRow != null) {
                currentRowKeyBeforeRollback = currentRow.getKey();
            }
        }
    }


    @Override
    public void afterRollback(TransactionEvent transactionEvent) {
        super.afterRollback(transactionEvent);
        if (currentRowKeyBeforeRollback != null) {
            executeQuery();
            findAndSetCurrentRowByKey(currentRowKeyBeforeRollback, 1);
        }
        currentRowKeyBeforeRollback = null;
    }

    @Override
    public void insertRow(Row row) {
        if ("true".equalsIgnoreCase((String)this.getProperty("NewRowAtEnd"))) {
            Row lastRow = this.last();
            if (lastRow != null) {
                // get index of last row
                int lastRowIdx = this.getRangeIndexOf(lastRow);
                // insert row after the last row
                this.insertRowAtRangeIndex(lastRowIdx, row);
                // set inserted row as the current row
                this.setCurrentRow(row);
            } else {
                super.insertRow(row);
            }
        } else {
            super.insertRow(row);
        }
    }
}

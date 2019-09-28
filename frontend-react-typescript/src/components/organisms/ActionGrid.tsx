import * as React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import DeleteIcon from '@material-ui/icons/DeleteOutlined';

import Paper from 'components/atoms/Paper';
import Table from 'components/atoms/Table';
import TableToolbar, { TableToolbarProps } from 'components/molecules/TableToolbar';
import TableHeadLayer, { TableColum } from 'components/molecules/TableHeadLayer';
import TableBody from 'components/atoms/TableBody';
import TableRow from 'components/atoms/TableRow';
import TableCell from 'components/atoms/TableCell';
import IconButton from 'components/atoms/IconButton';
import Checkbox from 'components/atoms/Checkbox';

export interface GridData {
  id: string;
  status: 'add' | 'delete' | 'default';
}

function desc<T extends GridData>(a: T, b: T, orderBy: keyof T) {
  if (b[orderBy] < a[orderBy]) {
    return -1;
  }
  if (b[orderBy] > a[orderBy]) {
    return 1;
  }
  return 0;
}

function stableSort<T extends GridData>(array: T[], cmp: (a: T, b: T) => number) {
  const stabilizedThis = array.map((el, index) => [el, index] as [T, number]);
  stabilizedThis.sort((a, b) => {
    const order = cmp(a[0], b[0]);
    if (order !== 0) return order;
    return a[1] - b[1];
  });
  return stabilizedThis.map(el => el[0]);
}

function getSorting<T extends GridData>(order: 'asc' | 'desc', orderBy: keyof T): (a: T, b: T) => number {
  return order === 'desc' ? (a, b) => desc(a, b, orderBy) : (a, b) => -desc(a, b, orderBy);
}

const useStyles = makeStyles(theme => ({
  root: {
    width: '100%',
    marginTop: theme.spacing(3),
  },
  table: {
    minWidth: 750,
  },
  tableWrapper: {
    overflowX: 'auto',
  },
}));

interface GridProps<T extends GridData> {
  columns: TableColum[];
  rows: T[];
  toolbarProps?: TableToolbarProps;
  order?: 'asc' | 'desc';
  orderBy?: keyof T;
  onClickRow?: (event: React.MouseEvent<unknown>, rowId: string) => void;
  onClickRowDeleteIcon?: (e: React.MouseEvent<unknown>, rowId: string) => void;
  onClickHeadLayerAddIcon?: (e: React.MouseEvent<unknown>) => void;
}

interface GridState<T extends GridData> {
  order: 'asc' | 'desc';
  orderBy: keyof T;
}

class Grid<T extends GridData> extends React.Component<GridProps<T>, GridState<T>> {
  constructor(props: GridProps<T>) {
    super(props);
    this.state = {
      order: this.props.order ? this.props.order : 'asc',
      orderBy: this.props.orderBy ? this.props.orderBy : 'id',
    };
  }

  handleRequestSort = (e: React.MouseEvent<unknown>, property: keyof T) => {
    const isDesc = this.state.orderBy === property && this.state.order === 'desc';

    this.setState({ order: isDesc ? 'asc' : 'desc', orderBy: property });
  };

  onClickRow = (rowId: string) => (e: React.MouseEvent<unknown>) => {
    if (this.props.onClickRow) {
      this.props.onClickRow(e, rowId);
    }
  };

  onClickRowDeleteIcon = (rowId: string) => (e: React.MouseEvent<unknown>) => {
    if (this.props.onClickRowDeleteIcon) {
      this.props.onClickRowDeleteIcon(e, rowId);
    }
  };

  render() {
    const classes = useStyles();
    return (
      <Paper className={classes.root}>
        {this.props.toolbarProps && <TableToolbar {...this.props.toolbarProps} />}
        <div className={classes.tableWrapper}>
          <Table className={classes.table} aria-labelledby="tableTitle">
            <TableHeadLayer
              columns={this.props.columns}
              order={this.state.order}
              orderBy={this.state.orderBy}
              onRequestSort={this.handleRequestSort}
              onClickAddIcon={this.props.onClickHeadLayerAddIcon}
            />
            <TableBody>
              {stableSort(this.props.rows, getSorting(this.state.order, this.state.orderBy)).map(row => {
                <TableRow hover onClick={this.onClickRow(row.id)} role="checkbox" tabIndex={-1} key={row.id}>
                  {this.props.onClickRowDeleteIcon && (
                    <TableCell>
                      <IconButton
                        icon={<DeleteIcon aria-label="delete" />}
                        onClick={this.onClickRowDeleteIcon(row.id)}
                      />
                    </TableCell>
                  )}
                  {this.props.columns.map(col => {
                    // @ts-ignore
                    const label = row[col.id];

                    return col.checkbox ? (
                      <TableCell align={col.align} key={col.id} padding="checkbox">
                        <Checkbox {...col.checkbox} />
                        {label}
                      </TableCell>
                    ) : (
                      <TableCell align={col.align} padding={col.disablePadding ? 'none' : 'default'} key={col.id}>
                        {label}
                      </TableCell>
                    );
                  })}
                </TableRow>;
              })}
            </TableBody>
          </Table>
        </div>
      </Paper>
    );
  }
}

export default Grid;

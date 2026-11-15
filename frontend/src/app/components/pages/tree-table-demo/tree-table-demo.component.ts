import { Component, signal } from '@angular/core';
import { BlockUIModule } from '@openng/optimus-ui/blockui';
import { ToolbarModule } from '@openng/optimus-ui/toolbar';
import { Column } from '../../library/table/column';
import { TreeTableComponent } from '../../library/table/tree-table/tree-table.component';
import { TreeNode } from '@openng/optimus-ui/api';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';

@Component({
  imports: [BlockUIModule, ToolbarModule, TreeTableComponent, TableFastSearchComponent],
  selector: 'pro-legacy-tree-table-demo',
  styleUrl: './tree-table-demo.component.css',
  templateUrl: './tree-table-demo.component.html',
})
export class TreeTableDemoComponent {
  protected readonly data = signal<TreeNode[]>([]);
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'ID',
      width: 120,
    },
    {
      type: 'boolean',
      field: 'flag',
      header: 'flag',
      width: 350,
    },
    {
      type: 'text',
      field: 'text',
      header: 'text',
      width: 280,
    },
    {
      type: 'date',
      field: 'date',
      header: 'date',
      width: 255,
    },
    {
      type: 'number',
      field: 'numeric',
      header: 'numeric',
    },
  ]);

  constructor() {
    const texts = [
      'Документ',
      'Заявка',
      'Пользователь',
      'Транзакция',
      'Операция',
      'Настройка',
      'Категория',
      'Элемент',
      'Запись',
      'Событие',
      'Проект',
      'Задача',
      'Отчёт',
      'Параметр',
      'Сущность',
      'Ресурс',
    ];

    const randomText = () => texts[Math.floor(Math.random() * texts.length)];

    const randomNumeric = () => Number((Math.random() * 10000).toFixed(2));

    const randomDate = (start = new Date(2000, 0, 1), end = new Date()) =>
      new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));

    let id = 1;

    const data = Array.from({ length: 20 }, () => {
      const parent: any = {
        id: id++,
        flag: Math.random() > 0.5,
        text: randomText(),
        numeric: randomNumeric(),
        date: randomDate(),
        children: [],
      };

      const children = Array.from({ length: 4 }, () => {
        const child = {
          id: id++,
          flag: Math.random() > 0.5,
          text: randomText(),
          numeric: randomNumeric(),
          date: randomDate(),
        };
        return {
          key: `${child.id}`,
          data: child,
        };
      });

      return { key: `${parent.id}`, data: parent, children: children };
    });
    this.data.set(data);
  }
}

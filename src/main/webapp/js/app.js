/* SAMS front-end helpers - no framework, no inline handlers (works with the strict CSP). */
(function () {
  'use strict';

  // confirm dialogs:  <form data-confirm="Are you sure?">
  document.addEventListener('submit', function (e) {
    var msg = e.target.getAttribute('data-confirm');
    if (msg && !window.confirm(msg)) e.preventDefault();
  });

  // auto-submit: <select data-autosubmit> / <input type=date data-autosubmit>
  document.querySelectorAll('[data-autosubmit]').forEach(function (el) {
    el.addEventListener('change', function () { el.form.submit(); });
  });

  // live table filter:  <input data-filter="#tableId">
  document.querySelectorAll('[data-filter]').forEach(function (input) {
    var table = document.querySelector(input.getAttribute('data-filter'));
    if (!table) return;
    input.addEventListener('input', function () {
      var q = input.value.toLowerCase();
      table.querySelectorAll('tbody tr').forEach(function (tr) {
        tr.classList.toggle('hidden', tr.textContent.toLowerCase().indexOf(q) === -1);
      });
    });
  });

  // show student-only fields only when role = STUDENT
  var role = document.querySelector('[data-role-select]');
  if (role) {
    var toggle = function () {
      document.querySelectorAll('.student-only').forEach(function (d) {
        d.classList.toggle('hidden', role.value !== 'STUDENT');
      });
    };
    role.addEventListener('change', toggle);
    toggle();
  }

  // attendance page: "mark all" buttons + live counters
  var form = document.getElementById('attForm');
  if (form) {
    var count = function () {
      var c = { PRESENT: 0, LATE: 0, ABSENT: 0 };
      form.querySelectorAll('input[type=radio]:checked').forEach(function (r) { c[r.value]++; });
      document.getElementById('cP').textContent = c.PRESENT;
      document.getElementById('cL').textContent = c.LATE;
      document.getElementById('cA').textContent = c.ABSENT;
    };
    document.querySelectorAll('[data-markall]').forEach(function (b) {
      b.addEventListener('click', function () {
        form.querySelectorAll('input[type=radio][value=' + b.getAttribute('data-markall') + ']').forEach(function (r) { r.checked = true; });
        count();
      });
    });
    form.addEventListener('change', count);
    count();
  }

  // copy button:  <button data-copy="#elementId">
  document.querySelectorAll('[data-copy]').forEach(function (b) {
    b.addEventListener('click', function () {
      var el = document.querySelector(b.getAttribute('data-copy'));
      if (el && navigator.clipboard) {
        navigator.clipboard.writeText(el.textContent.trim()).then(function () { b.textContent = 'Copied!'; });
      }
    });
  });

  // print button
  document.querySelectorAll('[data-print]').forEach(function (b) {
    b.addEventListener('click', function () { window.print(); });
  });

  // flash messages fade away after 6 seconds
  var flash = document.querySelector('.alert-success');
  if (flash) setTimeout(function () { flash.classList.add('hidden'); }, 6000);
})();

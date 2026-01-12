// 모바일 햄버거 토글
  (function () {
    const btn = document.querySelector('.nav-toggle');
    const nav = document.getElementById('global-nav');

    if (btn && nav) {
      btn.addEventListener('click', () => {
        const expanded = btn.getAttribute('aria-expanded') === 'true';
        btn.setAttribute('aria-expanded', String(!expanded));
        nav.classList.toggle('open');
        document.body.classList.toggle('nav-open');
      });
    }

	 // 서브메뉴 키보드 접근성 + 모바일 아코디언
	    const mq = window.matchMedia('(max-width: 992px)');
	    document.querySelectorAll('.menu-item.has-sub > .menu-link').forEach(link => {
	      link.addEventListener('keydown', e => {
	        if (e.key === 'Enter' || e.key === ' ') {
	          e.preventDefault();
	          link.parentElement.classList.toggle('sub-open');
	        }
	      });
	      link.addEventListener('click', e => {
	        if (mq.matches) {            // 모바일에서만 링크 이동 막고 토글
	          e.preventDefault();
	          link.parentElement.classList.toggle('sub-open');
	        }
	      });
	    });
	  })();

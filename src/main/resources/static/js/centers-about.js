// 페이지 전용 인터랙션(필요 시 확장)
// 예: 히어로 이미지 로딩 후 페이드인
document.addEventListener('DOMContentLoaded', () => {
  const ph = document.querySelector('.ci-hero__media .img-ph');
  if (ph) {
    ph.style.opacity = '0';
    ph.style.transition = 'opacity .4s ease';
    requestAnimationFrame(() => (ph.style.opacity = '1'));
  }
});
